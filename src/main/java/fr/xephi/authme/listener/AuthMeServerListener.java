package fr.xephi.authme.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListPingEvent;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;

public class AuthMeServerListener implements Listener {

    public AuthMe plugin;
    private Messages m = Messages.getInstance();

    public AuthMeServerListener(AuthMe plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerPing(ServerListPingEvent event) {
        if (!Settings.enableProtection)
            return;
        if (Settings.countries.isEmpty())
            return;
        if (!Settings.countriesBlacklist.isEmpty()) {
            if (Settings.countriesBlacklist.contains(plugin.getCountryCode(event.getAddress().getHostAddress())))
                event.setMotd(m._("country_banned")[0]);
        }
        if (Settings.countries.contains(plugin.getCountryCode(event.getAddress().getHostAddress()))) {
            event.setMotd(plugin.getServer().getMotd());
        } else {
            event.setMotd(m._("country_banned")[0]);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPluginDisable(PluginDisableEvent event) {
        String pluginName = event.getPlugin().getName();
        if (pluginName.equalsIgnoreCase("Essentials")) {
            plugin.ess = null;
            ConsoleLogger.info("Essentials has been disabled, unhook!");
            return;
        }
        if (pluginName.equalsIgnoreCase("EssentialsSpawn")) {
            plugin.essentialsSpawn = null;
            ConsoleLogger.info("EssentialsSpawn has been disabled, unhook!");
            return;
        }
        if (pluginName.equalsIgnoreCase("Multiverse-Core")) {
            plugin.multiverse = null;
            ConsoleLogger.info("Multiverse-Core has been disabled, unhook!");
            return;
        }
        if (pluginName.equalsIgnoreCase("Notifications")) {
            plugin.notifications = null;
            ConsoleLogger.info("Notifications has been disabled, unhook!");
        }
        if (pluginName.equalsIgnoreCase("ChestShop")) {
            plugin.ChestShop = 0;
            ConsoleLogger.info("ChestShop has been disabled, unhook!");
        }
        if (pluginName.equalsIgnoreCase("CombatTag")) {
            plugin.CombatTag = 0;
            ConsoleLogger.info("CombatTag has been disabled, unhook!");
        }
        if (pluginName.equalsIgnoreCase("Citizens")) {
            plugin.CitizensVersion = 0;
            ConsoleLogger.info("Citizens has been disabled, unhook!");
        }
        if (pluginName.equalsIgnoreCase("Vault")) {
            plugin.permission = null;
            ConsoleLogger.showError("Vault has been disabled, unhook permissions!");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPluginEnable(PluginEnableEvent event) {
        String pluginName = event.getPlugin().getName();
        if (pluginName.equalsIgnoreCase("Essentials") || pluginName.equalsIgnoreCase("EssentialsSpawn"))
            plugin.checkEssentials();
        if (pluginName.equalsIgnoreCase("Multiverse-Core"))
            plugin.checkMultiverse();
        if (pluginName.equalsIgnoreCase("Notifications"))
            plugin.checkNotifications();
        if (pluginName.equalsIgnoreCase("ChestShop"))
            plugin.checkChestShop();
        if (pluginName.equalsIgnoreCase("CombatTag"))
            plugin.combatTag();
        if (pluginName.equalsIgnoreCase("Citizens"))
            plugin.citizensVersion();
        if (pluginName.equalsIgnoreCase("Vault"))
            plugin.checkVault();
    }
}
