package fr.xephi.authme.listener;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.service.PluginHookService;
import fr.xephi.authme.listener.protocollib.ProtocolLibService;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.SpawnLoader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import javax.inject.Inject;

/**
 */
public class ServerListener implements Listener {

    @Inject
    private PluginHookService pluginHookService;
    @Inject
    private SpawnLoader spawnLoader;
    @Inject
    private ProtocolLibService protocolLibService;
    @Inject
    private PermissionsManager permissionsManager;

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
            pluginHookService.unhookEssentials();
            ConsoleLogger.info("Essentials has been disabled: unhooking");
        } else if ("CMI".equalsIgnoreCase(pluginName)) {
            pluginHookService.unhookCmi();
            spawnLoader.unloadCmiSpawn();
            ConsoleLogger.info("CMI has been disabled: unhooking");
        } else if ("Multiverse-Core".equalsIgnoreCase(pluginName)) {
            pluginHookService.unhookMultiverse();
            ConsoleLogger.info("Multiverse-Core has been disabled: unhooking");
        } else if ("EssentialsSpawn".equalsIgnoreCase(pluginName)) {
            spawnLoader.unloadEssentialsSpawn();
            ConsoleLogger.info("EssentialsSpawn has been disabled: unhooking");
        } else if ("ProtocolLib".equalsIgnoreCase(pluginName)) {
            protocolLibService.disable();
            ConsoleLogger.warning("ProtocolLib has been disabled, unhooking packet adapters!");
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
            pluginHookService.tryHookToEssentials();
        } else if ("Multiverse-Core".equalsIgnoreCase(pluginName)) {
            pluginHookService.tryHookToMultiverse();
        } else if ("EssentialsSpawn".equalsIgnoreCase(pluginName)) {
            spawnLoader.loadEssentialsSpawn();
        } else if ("CMI".equalsIgnoreCase(pluginName)) {
            pluginHookService.tryHookToCmi();
            spawnLoader.loadCmiSpawn();
        } else if ("ProtocolLib".equalsIgnoreCase(pluginName)) {
            protocolLibService.setup();
        }
    }
}
