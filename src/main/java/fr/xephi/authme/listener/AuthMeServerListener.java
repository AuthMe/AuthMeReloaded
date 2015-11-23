package fr.xephi.authme.listener;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.GeoLiteAPI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.Plugin;

/**
 */
public class AuthMeServerListener implements Listener {

    public AuthMe plugin;
    private Messages m = Messages.getInstance();

    /**
     * Constructor for AuthMeServerListener.
     *
     * @param plugin AuthMe
     */
    public AuthMeServerListener(AuthMe plugin) {
        this.plugin = plugin;
    }

    /**
     * Method onServerPing.
     *
     * @param event ServerListPingEvent
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerPing(ServerListPingEvent event) {
        if (!Settings.enableProtection)
            return;
        if (Settings.countries.isEmpty())
            return;
        if (!Settings.countriesBlacklist.isEmpty()) {
            if (Settings.countriesBlacklist.contains(GeoLiteAPI.getCountryCode(event.getAddress().getHostAddress())))
                event.setMotd(m.send("country_banned")[0]);
        }
        if (Settings.countries.contains(GeoLiteAPI.getCountryCode(event.getAddress().getHostAddress()))) {
            event.setMotd(plugin.getServer().getMotd());
        } else {
            event.setMotd(m.send("country_banned")[0]);
        }
    }

    /**
     * Method onPluginDisable.
     *
     * @param event PluginDisableEvent
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPluginDisable(PluginDisableEvent event) {
        // Get the plugin instance
        Plugin pluginInstance = event.getPlugin();

        // Make sure the plugin instance isn't null
        if (pluginInstance == null)
            return;

        // Make sure it's not this plugin itself
        if (pluginInstance.equals(this.plugin))
            return;

        // Call the onPluginDisable method in the permissions manager
        this.plugin.getPermissionsManager().onPluginDisable(event);

        String pluginName = pluginInstance.getName();
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
        if (pluginName.equalsIgnoreCase("CombatTagPlus")) {
            plugin.combatTagPlus = null;
            ConsoleLogger.info("CombatTagPlus has been disabled, unhook!");
        }
        if (pluginName.equalsIgnoreCase("ProtocolLib")) {
            plugin.inventoryProtector = null;
            ConsoleLogger.showError("ProtocolLib has been disabled, unhook packet inventory protection!");
        }
    }

    /**
     * Method onPluginEnable.
     *
     * @param event PluginEnableEvent
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPluginEnable(PluginEnableEvent event) {
        // Call the onPluginEnable method in the permissions manager
        this.plugin.getPermissionsManager().onPluginEnable(event);

        String pluginName = event.getPlugin().getName();
        if (pluginName.equalsIgnoreCase("Essentials") || pluginName.equalsIgnoreCase("EssentialsSpawn"))
            plugin.checkEssentials();
        if (pluginName.equalsIgnoreCase("Multiverse-Core"))
            plugin.checkMultiverse();
        if (pluginName.equalsIgnoreCase("CombatTagPlus"))
            plugin.checkCombatTagPlus();
        if (pluginName.equalsIgnoreCase("ProtocolLib")) {
            plugin.checkProtocolLib();
        }
    }
}
