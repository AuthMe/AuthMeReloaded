package fr.xephi.authme.bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;

final class BungeeReloadCommand extends Command {

    private final BungeeConfigManager configManager;
    private final BungeeProxyBridge proxyBridge;

    BungeeReloadCommand(BungeeConfigManager configManager, BungeeProxyBridge proxyBridge) {
        super("abreloadproxy", "authmebungee.reload");
        this.configManager = configManager;
        this.proxyBridge = proxyBridge;
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        proxyBridge.reload(configManager.reload());
        commandSender.sendMessage(
            new ComponentBuilder("AuthMeBungee configuration reloaded!").color(ChatColor.GREEN).create()
        );
    }
}
