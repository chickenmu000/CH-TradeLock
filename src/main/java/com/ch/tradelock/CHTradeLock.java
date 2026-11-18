package com.ch.tradelock;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class CHTradeLock extends JavaPlugin {

    private ConfigManager configManager;
    private TradeLockListener listener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        listener = new TradeLockListener(this);
        getServer().getPluginManager().registerEvents(listener, this);
        getServer().getPluginManager().registerEvents(new DeathProtectionListener(this), this);
        getLogger().info("CH-TradeLock v" + getDescription().getVersion() + " 활성화됨.");
    }

    @Override
    public void onDisable() {
        getLogger().info("CH-TradeLock 비활성화됨.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("tradelock")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                configManager.reload();
                sender.sendMessage("§aCH-TradeLock 설정이 리로드되었습니다.");
                return true;
            }
            sender.sendMessage("§e사용법: /tradelock reload");
            return true;
        }
        if (command.getName().equalsIgnoreCase("invsave")) {
            return handleInvSave(sender, args);
        }
        return false;
    }

    // /invsave set | clear | give [수량]
    private boolean handleInvSave(CommandSender sender, String[] args) {
        String usage = "§e사용법: /invsave <set|clear|give [수량]>";
        if (args.length == 0) {
            sender.sendMessage(usage);
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("clear")) {
            if (configManager.getTicketItem() == null) {
                sender.sendMessage("§c[인벤세이브권] 지정된 인벤세이브권이 없습니다.");
                return true;
            }
            getConfig().set("death-protection.ticket-item", null);
            saveConfig();
            configManager.reload();
            sender.sendMessage("§a[인벤세이브권] 인벤세이브권 지정을 해제했습니다.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c플레이어만 사용할 수 있습니다.");
            return true;
        }
        if (sub.equals("set")) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.getType().isAir()) {
                player.sendMessage("§c[인벤세이브권] 인벤세이브권으로 지정할 아이템을 손에 들어주세요.");
                return true;
            }
            ItemStack ticket = hand.clone();
            ticket.setAmount(1);
            getConfig().set("death-protection.ticket-item", ticket);
            saveConfig();
            configManager.reload();
            player.sendMessage("§a[인벤세이브권] 손에 든 아이템을 인벤세이브권으로 지정했습니다.");
            player.sendMessage("§7(이름, 로어, 재질이 같은 아이템만 인벤세이브권으로 인식됩니다)");
            return true;
        }
        if (sub.equals("give")) {
            ItemStack ticket = configManager.getTicketItem();
            if (ticket == null) {
                player.sendMessage("§c[인벤세이브권] 먼저 /invsave set 으로 아이템을 지정해 주세요.");
                return true;
            }
            int amount = 1;
            if (args.length >= 2) {
                try {
                    amount = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    amount = 0;
                }
            }
            if (amount < 1 || amount > 10000) {
                player.sendMessage("§c[인벤세이브권] 수량은 1 이상 10000 이하여야 합니다.");
                return true;
            }
            int left = amount;
            while (left > 0) {
                ItemStack stack = ticket.clone();
                int n = Math.min(left, stack.getMaxStackSize());
                stack.setAmount(n);
                for (ItemStack rest : player.getInventory().addItem(stack).values()) {
                    player.getWorld().dropItem(player.getLocation(), rest);
                }
                left -= n;
            }
            player.sendMessage("§a[인벤세이브권] 인벤세이브권 " + amount + "개를 지급받았습니다.");
            return true;
        }
        sender.sendMessage(usage);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("invsave") && args.length == 1) {
            for (String s : List.of("set", "clear", "give")) {
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
            }
        } else if (command.getName().equalsIgnoreCase("tradelock") && args.length == 1) {
            if ("reload".startsWith(args[0].toLowerCase())) out.add("reload");
        }
        return out;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
