package com.ch.tradelock;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ConfigManager {

    private final CHTradeLock plugin;
    private List<String> whitelistedGuis;
    private Set<String> blockedCommands;

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
    }

    public List<String> getWhitelistedGuis() {
        return whitelistedGuis;
    }

    public Set<String> getBlockedCommands() {
        return blockedCommands;
    }
}
