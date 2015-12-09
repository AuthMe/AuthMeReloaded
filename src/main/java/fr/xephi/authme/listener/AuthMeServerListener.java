package fr.xephi.authme.listener;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
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

    private final AuthMe plugin;
    private final Messages m;

    /**
     * Constructor for AuthMeServerListener.
     *
     * @param plugin AuthMe
     */
    public AuthMeServerListener(AuthMe plugin) {
        this.m = plugin.getMessages();
        this.plugin = plugin;
    }

    /**
     * Method onServerPing.
     *
     * @param event ServerListPingEvent
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerPing(ServerListPingEvent event) {
        if (!Settings.enableProtection) {
            return;
        }

        String countryCode = GeoLiteAPI.getCountryCode(event.getAddress().getHostAddress());
        if (!Settings.countriesBlacklist.isEmpty() && Settings.countriesBlacklist.contains(countryCode)) {
            event.setMotd(m.retrieveSingle(MessageKey.COUNTRY_BANNED_ERROR));
            return;
        }

        if (!Settings.countries.isEmpty() && !Settings.countries.contains(countryCode)) {
            event.setMotd(m.retrieveSingle(MessageKey.COUNTRY_BANNED_ERROR));
        }
    }

    /**
     * Method onPluginDisable.
     *
     * @param event PluginDisableEvent
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPluginDisable(PluginDisableEvent event) {
        // Make sure the plugin instance isn't null
        if (event.getPlugin() == null) {
            return;
        }

        // Get the plugin instance
        Plugin pluginInstance = event.getPlugin();

        // Make sure it's not this plugin itself
        if (pluginInstance.equals(this.plugin)) {
            return;
        }

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
            return;
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
        if (pluginName.equalsIgnoreCase("Essentials") || pluginName.equalsIgnoreCase("EssentialsSpawn")) {
            plugin.checkEssentials();
            return;
        }
        if (pluginName.equalsIgnoreCase("Multiverse-Core")) {
            plugin.checkMultiverse();
            return;
        }
        if (pluginName.equalsIgnoreCase("CombatTagPlus")) {
            plugin.checkCombatTagPlus();
            return;
        }
        if (pluginName.equalsIgnoreCase("ProtocolLib")) {
            plugin.checkProtocolLib();
        }
    }
}
