package fr.xephi.authme.listener.protocollib;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.util.BukkitService;
import org.bukkit.entity.Player;

import javax.inject.Inject;

public class ProtocolLibService implements SettingsDependent {

    /* Packet Adapters */
    private AuthMeInventoryPacketAdapter inventoryPacketAdapter;
    private AuthMeTabCompletePacketAdapter tabCompletePacketAdapter;
    private AuthMeTablistPacketAdapter tablistPacketAdapter;

    /* Settings */
    private boolean protectInvBeforeLogin;
    private boolean denyTabCompleteBeforeLogin;
    private boolean hideTablistBeforeLogin;

    /* Service */
    private boolean isEnabled;
    private AuthMe plugin;
    private BukkitService bukkitService;

    @Inject
    ProtocolLibService(AuthMe plugin, BukkitService bukkitService, NewSetting settings) {
        this.plugin = plugin;
        this.bukkitService = bukkitService;
        loadSettings(settings);
        setup();
    }

    /**
     * Set up the ProtocolLib packet adapters.
     */
    public void setup() {
        // Check if ProtocolLib is enabled on the server.
        if (!plugin.getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
            if (protectInvBeforeLogin) {
                ConsoleLogger.showError("WARNING! The protectInventory feature requires ProtocolLib! Disabling it...");
            }
            if (denyTabCompleteBeforeLogin) {
                ConsoleLogger.showError("WARNING! The denyTabComplete feature requires ProtocolLib! Disabling it...");
            }
            if (hideTablistBeforeLogin) {
                ConsoleLogger.showError("WARNING! The hideTablist feature requires ProtocolLib! Disabling it...");
            }

            this.isEnabled = false;
            return;
        }

        // Set up packet adapters
        if (protectInvBeforeLogin && inventoryPacketAdapter == null) {
            inventoryPacketAdapter = new AuthMeInventoryPacketAdapter(plugin);
            inventoryPacketAdapter.register();
        } else if (inventoryPacketAdapter != null) {
            inventoryPacketAdapter.unregister();
            inventoryPacketAdapter = null;
        }
        if (denyTabCompleteBeforeLogin && tabCompletePacketAdapter == null) {
            tabCompletePacketAdapter = new AuthMeTabCompletePacketAdapter(plugin);
            tabCompletePacketAdapter.register();
        } else if (tabCompletePacketAdapter != null) {
            tabCompletePacketAdapter.unregister();
            tabCompletePacketAdapter = null;
        }
        if (hideTablistBeforeLogin && tablistPacketAdapter == null) {
            tablistPacketAdapter = new AuthMeTablistPacketAdapter(plugin, bukkitService);
            tablistPacketAdapter.register();
        } else if (tablistPacketAdapter != null) {
            tablistPacketAdapter.unregister();
            tablistPacketAdapter = null;
        }

        this.isEnabled = true;
    }

    public void disable() {
        isEnabled = false;

        if (inventoryPacketAdapter != null) {
            inventoryPacketAdapter.unregister();
            inventoryPacketAdapter = null;
        }
        if (tabCompletePacketAdapter != null) {
            tabCompletePacketAdapter.unregister();
            tabCompletePacketAdapter = null;
        }
        if (tablistPacketAdapter != null) {
            tablistPacketAdapter.unregister();
            tablistPacketAdapter = null;
        }
    }

    /**
     * Send a packet to the player to give them an inventory.
     *
     * @param player The player to send the packet to.
     */
    public void sendInventoryPacket(Player player) {
        if (isEnabled && inventoryPacketAdapter != null) {
            inventoryPacketAdapter.sendInventoryPacket(player);
        }
    }

    /**
     * Send a packet to the player to give them a blank inventory.
     *
     * @param player The player to send the packet to.
     */
    public void sendBlankInventoryPacket(Player player) {
        if (isEnabled && inventoryPacketAdapter != null) {
            inventoryPacketAdapter.sendBlankInventoryPacket(player);
        }
    }

    /**
     * Send a tab list packet to a player.
     *
     * @param player The player to send the packet to.
     */
    public void sendTabList(Player player) {
        if (isEnabled && tablistPacketAdapter != null) {
            tablistPacketAdapter.sendTablist(player);
        }
    }

    @Override
    public void loadSettings(NewSetting settings) {
        this.protectInvBeforeLogin = settings.getProperty(RestrictionSettings.PROTECT_INVENTORY_BEFORE_LOGIN);
        this.denyTabCompleteBeforeLogin = settings.getProperty(RestrictionSettings.DENY_TABCOMPLETE_BEFORE_LOGIN);
        this.hideTablistBeforeLogin = settings.getProperty(RestrictionSettings.HIDE_TABLIST_BEFORE_LOGIN);
    }
}
