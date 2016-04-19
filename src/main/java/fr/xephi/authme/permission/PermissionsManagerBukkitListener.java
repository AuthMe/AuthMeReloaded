package fr.xephi.authme.permission;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;

public class PermissionsManagerBukkitListener implements Listener {

    /**
     * The permissions manager instance.
     */
    private PermissionsManager permissionsManager;

    /**
     * Whether the listener is enabled or not.
     */
    private boolean enabled = true;

    /**
     * Constructor.\
     *
     * @param permissionsManager Permissions manager instance.
     */
    public PermissionsManagerBukkitListener(PermissionsManager permissionsManager) {
        this.permissionsManager = permissionsManager;
    }

    /**
     * Check whether the listener is enabled.
     *
     * @return True if the listener is enabled.
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Set whether the listener is enabled.
     * Disabling the listener will stop the event handling until it's enabled again.
     *
     * @param enabled True if enabled, false if disabled.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Called when a plugin is enabled.
     *
     * @param event Event reference.
     */
    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        // Make sure the listener is enabled
        if(!isEnabled())
            return;

        // Make sure the permissions manager is set
        if(this.permissionsManager == null)
            return;

        // Call the onPluginEnable method in the permissions manager
        permissionsManager.onPluginEnable(event);
    }

    /**
     * Called when a plugin is disabled.
     *
     * @param event Event reference.
     */
    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        // Make sure the listener is enabled
        if(!isEnabled())
            return;

        // Make sure the permissions manager is set
        if(this.permissionsManager == null)
            return;

        // Call the onPluginDisable method in the permissions manager
        permissionsManager.onPluginDisable(event);
    }
}
