package com.ch.tradelock;

import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ConfigManager {

    private final CHTradeLock plugin;
    private List<String> whitelistedGuis;
    private Set<String> blockedCommands;
    private Set<String> protectedWorlds;
    private boolean consumeTicket;

    public ConfigManager(CHTradeLock plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        whitelistedGuis = plugin.getConfig().getStringList("whitelisted-guis");
        blockedCommands = plugin.getConfig().getStringList("blocked-commands")
                .stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        protectedWorlds = new HashSet<>(plugin.getConfig().getStringList("death-protection.protected-worlds"));
        consumeTicket = plugin.getConfig().getBoolean("death-protection.consume-ticket", true);
    }

    public List<String> getWhitelistedGuis() {
        return whitelistedGuis;
    }

    public Set<String> getBlockedCommands() {
        return blockedCommands;
    }

    public Set<String> getProtectedWorlds() {
        return protectedWorlds;
    }

    public boolean isConsumeTicket() {
        return consumeTicket;
    }

    /** 인벤세이브권 아이템 (지정 안 됐으면 null) */
    public ItemStack getTicketItem() {
        ItemStack it = plugin.getConfig().getItemStack("death-protection.ticket-item");
        return it == null ? null : it.clone();
    }
}
