package com.ch.tradelock;

import net.kyori.adventure.text.Component;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Iterator;

/**
 * 사망 처리 (한 곳에서 순서대로 처리해서 아이템 복사/손실이 생기지 않게 함)
 *  1) 보호 월드에서 사망      -> 인벤토리 / 경험치 유지
 *  2) 초심자의 행운(총 플레이 시간 미만) -> 인벤토리 / 경험치 유지 (티켓 소모 없음)
 *  3) 인벤세이브권 소지 사망   -> 인벤토리 / 경험치 유지 (+ 티켓 1개 소모)
 *  4) 그 외                  -> 거래불가 아이템만 보호 (리스폰 후 돌려받음)
 */
public class DeathProtectionListener implements Listener {

    private static final Component MSG_WORLD = Component.text(
            "§a[월드 보호] 지정된 보호 월드에서 사망하여 아이템과 경험치가 안전하게 유지되었습니다.");
    private static final String MSG_BEGINNER = "§a[초심자의 행운] 아이템과 경험치를 잃지 않았습니다! §7(남은 시간 약 %d분)";
    private static final Component MSG_TICKET_USED = Component.text(
            "§a[인벤세이브권] 인벤세이브권 1개를 사용하여 아이템과 경험치를 보호했습니다.");
    private static final Component MSG_TICKET_EFFECT = Component.text(
            "§a[인벤세이브권] 인벤세이브권의 효과로 아이템과 경험치를 보호했습니다.");

    private final CHTradeLock plugin;

    public DeathProtectionListener(CHTradeLock plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        ConfigManager cfg = plugin.getConfigManager();
        boolean debug = cfg.isDebug();

        // 다른 플러그인/게임룰이 이미 인벤토리 유지를 켰다면 그대로 둠 (티켓도 소모하지 않음, 복사 방지)
        if (event.getKeepInventory()) {
            if (debug) plugin.getLogger().info("[사망디버그] " + player.getName() + " 월드=" + player.getWorld().getName()
                    + " : 이미 keepInventory=true 라서 처리 건너뜀 (다른 플러그인/게임룰이 먼저 켬)");
            return;
        }

        // 1) 보호 월드
        if (cfg.getProtectedWorlds().contains(player.getWorld().getName())) {
            protect(event);
            player.sendMessage(MSG_WORLD);
            return;
        }

        // 2) 초심자의 행운 (총 플레이 시간이 설정 시간 미만)
        double hours = cfg.getBeginnerHours();
        if (hours > 0) {
            long playedTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
            long limitTicks = (long) (hours * 72000L);
            if (playedTicks < limitTicks) {
                protect(event);
                long leftMin = (limitTicks - playedTicks) / 1200L;
                player.sendMessage(Component.text(String.format(MSG_BEGINNER, leftMin)));
                return;
            }
        }

        // 3) 인벤세이브권
        ItemStack ticket = cfg.getTicketItem();
        if (debug) plugin.getLogger().info("[사망디버그] " + player.getName() + " 월드=" + player.getWorld().getName()
                + " op=" + player.isOp() + " 티켓지정=" + (ticket != null));
        if (ticket != null) {
            PlayerInventory inv = player.getInventory();
            int slot = findTicket(inv, ticket);
            if (debug) {
                plugin.getLogger().info("[사망디버그] 티켓 슬롯=" + slot);
                if (slot < 0) logSameTypeItems(player, ticket);
            }
            if (slot >= 0) {
                protect(event);
                if (cfg.isConsumeTicket()) {
                    consumeOne(inv, slot);
                    player.sendMessage(MSG_TICKET_USED);
                } else {
                    player.sendMessage(MSG_TICKET_EFFECT);
                }
                return;
            }
        }

        // 4) 거래불가 아이템은 사망해도 떨어지지 않고, 리스폰 후 인벤토리로 돌아온다.
        Iterator<ItemStack> it = event.getDrops().iterator();
        while (it.hasNext()) {
            ItemStack drop = it.next();
            if (TradeLockListener.isLackItem(drop)) {
                it.remove();
                event.getItemsToKeep().add(drop);
            }
        }
    }

    /** 인벤토리/경험치 유지. getDrops().clear() 를 같이 해야 아이템이 복사되지 않는다. */
    private void protect(PlayerDeathEvent event) {
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
    }

    /**
     * 인벤세이브권 위치 찾기.
     * 1순위 : 완전히 같은 아이템(isSimilar)
     * 2순위 : 재질 + 이름 + 커스텀 모델 데이터가 같은 아이템 (상점/우편함 등을 거치며 NBT 가 조금 달라진 경우 대비)
     */
    private int findTicket(PlayerInventory inv, ItemStack ticket) {
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it != null && !it.getType().isAir() && it.isSimilar(ticket)) return i;
        }
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it != null && !it.getType().isAir() && looseMatch(it, ticket)) return i;
        }
        return -1;
    }

    private boolean looseMatch(ItemStack a, ItemStack b) {
        if (a.getType() != b.getType()) return false;
        ItemMeta ma = a.getItemMeta();
        ItemMeta mb = b.getItemMeta();
        if (ma == null || mb == null) return false;
        if (!ma.hasDisplayName() || !mb.hasDisplayName()) return false;
        String na = PlainTextComponentSerializer.plainText().serialize(ma.displayName());
        String nb = PlainTextComponentSerializer.plainText().serialize(mb.displayName());
        if (!na.equals(nb)) return false;
        int ca = ma.hasCustomModelData() ? ma.getCustomModelData() : -1;
        int cb = mb.hasCustomModelData() ? mb.getCustomModelData() : -1;
        return ca == cb;
    }

    /** 디버그 : 티켓과 재질이 같은 인벤토리 아이템의 이름/모델 정보를 출력 */
    private void logSameTypeItems(Player player, ItemStack ticket) {
        for (ItemStack it : player.getInventory().getContents()) {
            if (it == null || it.getType() != ticket.getType()) continue;
            ItemMeta m = it.getItemMeta();
            String name = (m != null && m.hasDisplayName())
                    ? PlainTextComponentSerializer.plainText().serialize(m.displayName()) : "(이름없음)";
            plugin.getLogger().info("[사망디버그] 같은 재질 아이템: 이름=" + name
                    + " 모델=" + (m != null && m.hasCustomModelData() ? m.getCustomModelData() : -1)
                    + " isSimilar=" + it.isSimilar(ticket));
        }
    }

    private void consumeOne(PlayerInventory inv, int slot) {
        ItemStack it = inv.getItem(slot);
        if (it == null) return;
        if (it.getAmount() <= 1) {
            inv.setItem(slot, null);
        } else {
            ItemStack rest = it.clone();
            rest.setAmount(it.getAmount() - 1);
            inv.setItem(slot, rest);
        }
    }
}
