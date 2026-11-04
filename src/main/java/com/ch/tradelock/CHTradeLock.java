package com.ch.tradelock;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class CHTradeLock extends JavaPlugin {

    private ConfigManager configManager;
    private TradeLockListener listener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        listener = new TradeLockListener(this);
        getServer().getPluginManager().registerEvents(listener, this);
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
        return false;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
