package fr.xephi.authme.listener;

import fr.xephi.authme.AuthMe;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
/**
 */
public class AuthMePluginListener implements Listener {

    /** Plugin instance. */
    public AuthMe instance;

    /**
     * Constructor.
     *
     * @param instance Main plugin instance.
     */
    public AuthMePluginListener(AuthMe instance) {
        this.instance = instance;
    }

    /**
     * Called when a plugin is enabled.
     *
     * @param event Event reference.
     */
    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        // Call the onPluginEnable method in the permissions manager
        this.instance.getPermissionsManager().onPluginEnable(event);
    }

    /**
     * Called when a plugin is disabled.
     *
     * @param event Event reference.
     */
    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        // Get the plugin instance
        Plugin plugin = event.getPlugin();

        // Make sure the plugin instance isn't null
        if(plugin == null)
            return;

        // Make sure it's not this plugin itself
        if(plugin.equals(this.instance))
            return;

        // Call the onPluginDisable method in the permissions manager
        this.instance.getPermissionsManager().onPluginDisable(event);
    }
}
