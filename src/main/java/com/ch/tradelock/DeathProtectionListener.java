package com.ch.tradelock;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Iterator;

/**
 * 사망 처리 (한 곳에서 순서대로 처리해서 아이템 복사/손실이 생기지 않게 함)
 *  1) 보호 월드에서 사망      -> 인벤토리 / 경험치 유지
 *  2) 인벤세이브권 소지 사망   -> 인벤토리 / 경험치 유지 (+ 티켓 1개 소모)
 *  3) 그 외                  -> 거래불가 아이템만 보호 (리스폰 후 돌려받음)
 */
public class DeathProtectionListener implements Listener {

    private static final Component MSG_WORLD = Component.text(
            "§a[월드 보호] 지정된 보호 월드에서 사망하여 아이템과 경험치가 안전하게 유지되었습니다.");
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
        // 다른 플러그인/게임룰이 이미 인벤토리 유지를 켰다면 그대로 둠 (티켓도 소모하지 않음, 복사 방지)
        if (event.getKeepInventory()) return;

        Player player = event.getEntity();
        ConfigManager cfg = plugin.getConfigManager();

        // 1) 보호 월드
        if (cfg.getProtectedWorlds().contains(player.getWorld().getName())) {
            protect(event);
            player.sendMessage(MSG_WORLD);
            return;
        }

        // 2) 인벤세이브권
        ItemStack ticket = cfg.getTicketItem();
        if (ticket != null) {
            PlayerInventory inv = player.getInventory();
            int slot = findTicket(inv, ticket);
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

        // 3) 거래불가 아이템은 사망해도 떨어지지 않고, 리스폰 후 인벤토리로 돌아온다.
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

    private int findTicket(PlayerInventory inv, ItemStack ticket) {
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it != null && !it.getType().isAir() && it.isSimilar(ticket)) return i;
        }
        return -1;
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
