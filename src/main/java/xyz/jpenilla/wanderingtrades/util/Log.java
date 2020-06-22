package xyz.jpenilla.wanderingtrades.util;

import xyz.jpenilla.jmplib.TextUtil;
import xyz.jpenilla.wanderingtrades.WanderingTrades;

public class Log {
    private final WanderingTrades plugin;

    public Log(WanderingTrades wanderingTrades) {
        plugin = wanderingTrades;
    }

    public void info(String s) {
        plugin.getLogger().info(TextUtil.colorize(s));
    }

    public void warn(String s) {
        plugin.getLogger().warning(TextUtil.colorize(s));
    }

    public void err(String s) {
        plugin.getLogger().severe(TextUtil.colorize(s));
    }

    public void debug(String s) {
        if(plugin.getCfg().isDebug()) {
            plugin.getLogger().info(TextUtil.colorize("&e[DEBUG] &r" + s));
        }
    }
}