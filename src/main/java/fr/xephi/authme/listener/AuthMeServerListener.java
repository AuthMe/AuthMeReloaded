package fr.xephi.authme.listener;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.hooks.PluginHooks;
import fr.xephi.authme.listener.protocollib.ProtocolLibService;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.settings.properties.ProtectionSettings;
import fr.xephi.authme.util.ValidationService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListPingEvent;

import javax.inject.Inject;

/**
 */
public class AuthMeServerListener implements Listener {

    @Inject
    private Messages messages;
    @Inject
    private NewSetting settings;
    @Inject
    private PluginHooks pluginHooks;
    @Inject
    private SpawnLoader spawnLoader;
    @Inject
    private ProtocolLibService protocolLibService;
    @Inject
    private ValidationService validationService;
    @Inject
    private PermissionsManager permissionsManager;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerPing(ServerListPingEvent event) {
        if (settings.getProperty(ProtectionSettings.ENABLE_PROTECTION)) {
            String playerIp = event.getAddress().getHostAddress();
            if (!validationService.isCountryAdmitted(playerIp)) {
                event.setMotd(messages.retrieveSingle(MessageKey.COUNTRY_BANNED_ERROR));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPluginDisable(PluginDisableEvent event) {
        // Make sure the plugin instance isn't null
        if (event.getPlugin() == null) {
            return;
        }

        final String pluginName = event.getPlugin().getName();

        // Call the onPluginDisable method in the permissions manager
        permissionsManager.onPluginDisable(pluginName);

        if ("Essentials".equalsIgnoreCase(pluginName)) {
            pluginHooks.unhookEssentials();
            ConsoleLogger.info("Essentials has been disabled: unhooking");
        } else if ("Multiverse-Core".equalsIgnoreCase(pluginName)) {
            pluginHooks.unhookMultiverse();
            ConsoleLogger.info("Multiverse-Core has been disabled: unhooking");
        } else if ("CombatTagPlus".equalsIgnoreCase(pluginName)) {
            pluginHooks.unhookCombatPlus();
            ConsoleLogger.info("CombatTagPlus has been disabled: unhooking");
        } else if ("EssentialsSpawn".equalsIgnoreCase(pluginName)) {
            spawnLoader.unloadEssentialsSpawn();
            ConsoleLogger.info("EssentialsSpawn has been disabled: unhooking");
        }

        if (pluginName.equalsIgnoreCase("ProtocolLib")) {
            protocolLibService.disable();
            ConsoleLogger.showError("ProtocolLib has been disabled, unhooking packet adapters!");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPluginEnable(PluginEnableEvent event) {
        // Make sure the plugin instance isn't null
        if (event.getPlugin() == null) {
            return;
        }

        final String pluginName = event.getPlugin().getName();

        // Call the onPluginEnable method in the permissions manager
        permissionsManager.onPluginEnable(pluginName);

        if ("Essentials".equalsIgnoreCase(pluginName)) {
            pluginHooks.tryHookToEssentials();
        } else if ("Multiverse-Core".equalsIgnoreCase(pluginName)) {
            pluginHooks.tryHookToMultiverse();
        } else if ("CombatTagPlus".equalsIgnoreCase(pluginName)) {
            pluginHooks.tryHookToCombatPlus();
        } else if ("EssentialsSpawn".equalsIgnoreCase(pluginName)) {
            spawnLoader.loadEssentialsSpawn();
        }

        if (pluginName.equalsIgnoreCase("ProtocolLib")) {
            protocolLibService.setup();
        }
    }
}
