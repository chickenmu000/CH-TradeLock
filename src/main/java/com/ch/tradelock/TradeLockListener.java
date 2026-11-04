package com.ch.tradelock;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Iterator;
import java.util.List;

public class TradeLockListener implements Listener {

    private static final String LACK_KEYWORD = "거래불가";

    private static final Component MSG_CANT_DROP = Component.text("§c거래불가 아이템은 버릴 수 없습니다.");
    private static final Component MSG_CANT_MOVE = Component.text("§c거래불가 아이템은 이 인벤토리에 넣을 수 없습니다.");
    private static final Component MSG_CANT_PICKUP = Component.text("§c거래불가 아이템은 주울 수 없습니다.");
    private static final Component MSG_CANT_COMMAND = Component.text("§c거래불가 아이템을 손에 들고 해당 명령어를 사용할 수 없습니다.");

    private final CHTradeLock plugin;

    public TradeLockListener(CHTradeLock plugin) {
        this.plugin = plugin;
    }

    boolean isLackItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        List<Component> lore = meta.lore();
        if (lore == null) return false;
        PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();
        for (Component line : lore) {
            if (serializer.serialize(line).contains(LACK_KEYWORD)) return true;
        }
        return false;
    }

    private boolean hasLackItemInHand(Player player) {
        return isLackItem(player.getInventory().getItemInMainHand())
                || isLackItem(player.getInventory().getItemInOffHand());
    }

    private boolean isAllowed(Inventory inventory, InventoryView view) {
        InventoryType type = inventory.getType();
        if (type == InventoryType.PLAYER || type == InventoryType.CRAFTING) return true;
        String title = PlainTextComponentSerializer.plainText().serialize(view.title());
        for (String allowed : plugin.getConfigManager().getWhitelistedGuis()) {
            if (title.contains(allowed)) return true;
        }
        return false;
    }

    // ---------------- 사망 시 보호 ----------------
    // 거래불가 아이템은 사망해도 떨어지지 않고, 리스폰 후 인벤토리로 돌아온다.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Iterator<ItemStack> it = event.getDrops().iterator();
        while (it.hasNext()) {
            ItemStack drop = it.next();
            if (isLackItem(drop)) {
                it.remove();
                event.getItemsToKeep().add(drop);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!hasLackItemInHand(player)) return;
        String label = event.getMessage().substring(1).split(" ")[0].toLowerCase();
        if (plugin.getConfigManager().getBlockedCommands().contains(label)) {
            event.setCancelled(true);
            player.sendMessage(MSG_CANT_COMMAND);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (isLackItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(MSG_CANT_DROP);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (isLackItem(event.getItem().getItemStack())) {
            event.setCancelled(true);
            player.sendMessage(MSG_CANT_PICKUP);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        InventoryView view = event.getView();
        InventoryAction action = event.getAction();
        Inventory clicked = event.getClickedInventory();
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        // 허용되지 않은 인벤토리 안에서 거래불가 아이템을 건드리는 경우
        if (clicked != null && !isAllowed(clicked, view)) {
            if (isLackItem(cursor) || isLackItem(current)) {
                cancelMove(event, player);
                return;
            }
        }

        // 커서의 거래불가 아이템을 허용되지 않은 인벤토리에 놓는 경우
        if (isLackItem(cursor) && clicked != null && !isAllowed(clicked, view)) {
            cancelMove(event, player);
            return;
        }

        if (isLackItem(current)) {
            // 쉬프트 클릭으로 반대편 인벤토리로 이동
            if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                Inventory target = (clicked != null && clicked.equals(view.getBottomInventory()))
                        ? view.getTopInventory()
                        : view.getBottomInventory();
                if (!isAllowed(target, view)) {
                    cancelMove(event, player);
                    return;
                }
            }
            if ((action == InventoryAction.HOTBAR_SWAP || action == InventoryAction.HOTBAR_MOVE_AND_READD)
                    && clicked != null && !isAllowed(clicked, view)) {
                cancelMove(event, player);
                return;
            }
        }

        // 핫바 숫자키 / 오프핸드 키로 거래불가 아이템을 허용되지 않은 인벤토리로 보내는 경우
        if ((action == InventoryAction.HOTBAR_SWAP || action == InventoryAction.HOTBAR_MOVE_AND_READD)
                && clicked != null && !isAllowed(clicked, view)) {
            int button = event.getHotbarButton();
            ItemStack hotbarItem = button >= 0
                    ? player.getInventory().getItem(button)
                    : player.getInventory().getItemInOffHand();
            if (isLackItem(hotbarItem)) {
                cancelMove(event, player);
            }
        }
    }

    // 인벤토리를 열지 않은 상태의 양손 바꾸기(F)는 허용한다.
    // 상자 등을 연 상태에서 F 키로 넣는 것은 onInventoryClick (HOTBAR_SWAP, hotbarButton -1) 에서 막는다.

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isLackItem(event.getOldCursor())) return;
        InventoryView view = event.getView();
        int topSize = view.getTopInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize && !isAllowed(view.getTopInventory(), view)) {
                cancelMove(event, player);
                return;
            }
        }
    }

    private void cancelMove(Cancellable event, Player player) {
        event.setCancelled(true);
        player.sendMessage(MSG_CANT_MOVE);
    }
}
