package fr.xephi.authme.listener;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.Utils;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListPingEvent;

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
            if (Settings.countriesBlacklist.contains(Utils.getCountryCode(event.getAddress().getHostAddress())))
                event.setMotd(m.send("country_banned")[0]);
        }
        if (Settings.countries.contains(Utils.getCountryCode(event.getAddress().getHostAddress()))) {
            event.setMotd(plugin.getServer().getMotd());
        } else {
            event.setMotd(m.send("country_banned")[0]);
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
        if (pluginName.equalsIgnoreCase("ChestShop")) {
            plugin.legacyChestShop = false;
            ConsoleLogger.info("ChestShop has been disabled, unhook!");
        }
        if (pluginName.equalsIgnoreCase("CombatTagPlus")) {
            plugin.combatTagPlus = null;
            ConsoleLogger.info("CombatTagPlus has been disabled, unhook!");
        }
        if (pluginName.equalsIgnoreCase("Vault")) {
            plugin.permission = null;
            ConsoleLogger.showError("Vault has been disabled, unhook permissions!");
        }
        if (pluginName.equalsIgnoreCase("ProtocolLib")) {
            plugin.inventoryProtector = null;
            ConsoleLogger.showError("ProtocolLib has been disabled, unhook packet inventory protection!");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPluginEnable(PluginEnableEvent event) {
        String pluginName = event.getPlugin().getName();
        if (pluginName.equalsIgnoreCase("Essentials") || pluginName.equalsIgnoreCase("EssentialsSpawn"))
            plugin.checkEssentials();
        if (pluginName.equalsIgnoreCase("Multiverse-Core"))
            plugin.checkMultiverse();
        if (pluginName.equalsIgnoreCase("ChestShop"))
            plugin.checkChestShop();
        if (pluginName.equalsIgnoreCase("CombatTagPlus"))
            plugin.checkCombatTagPlus();
        if (pluginName.equalsIgnoreCase("Vault"))
            plugin.checkVault();
        if (pluginName.equalsIgnoreCase("ProtocolLib")) {
            plugin.checkProtocolLib();
        }
    }
}
