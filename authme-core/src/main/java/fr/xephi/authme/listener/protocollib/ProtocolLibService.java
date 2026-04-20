package fr.xephi.authme.listener.protocollib;

import ch.jalu.injector.annotations.NoFieldScan;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.bukkit.entity.Player;

import javax.inject.Inject;

@NoFieldScan
public class ProtocolLibService implements SettingsDependent {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(ProtocolLibService.class);

    /* Packet Adapters */
    private InventoryPacketAdapter inventoryPacketAdapter;
    private TabCompletePacketAdapter tabCompletePacketAdapter;

    /* Settings */
    private boolean protectInvBeforeLogin;
    private boolean denyTabCompleteBeforeLogin;

    /* Service */
    private boolean isEnabled;
    private final AuthMe plugin;
    private final BukkitService bukkitService;
    private final PlayerCache playerCache;
    private final DataSource dataSource;

    @Inject
    ProtocolLibService(AuthMe plugin, Settings settings, BukkitService bukkitService, PlayerCache playerCache,
                       DataSource dataSource) {
        this.plugin = plugin;
        this.bukkitService = bukkitService;
        this.playerCache = playerCache;
        this.dataSource = dataSource;
        reload(settings);
    }

    /**
     * Set up the ProtocolLib packet adapters.
     */
    public void setup() {
        // Check if ProtocolLib is enabled on the server.
        if (!plugin.getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
            if (protectInvBeforeLogin) {
                logger.warning("WARNING! The protectInventory feature requires ProtocolLib! Disabling it...");
            }

            if (denyTabCompleteBeforeLogin) {
                logger.warning("WARNING! The denyTabComplete feature requires ProtocolLib! Disabling it...");
            }

            this.isEnabled = false;
            return;
        }

        // Set up packet adapters
        if (protectInvBeforeLogin) {
            if (inventoryPacketAdapter == null) {
                // register the packet listener and start hiding it for all already online players (reload)
                inventoryPacketAdapter = new InventoryPacketAdapter(plugin, playerCache, dataSource);
                inventoryPacketAdapter.register(bukkitService);
            }
        } else if (inventoryPacketAdapter != null) {
            inventoryPacketAdapter.unregister();
            inventoryPacketAdapter = null;
        }

        if (denyTabCompleteBeforeLogin) {
            if (tabCompletePacketAdapter == null) {
                tabCompletePacketAdapter = new TabCompletePacketAdapter(plugin, playerCache);
                tabCompletePacketAdapter.register();
            }
        } else if (tabCompletePacketAdapter != null) {
            tabCompletePacketAdapter.unregister();
            tabCompletePacketAdapter = null;
        }

        this.isEnabled = true;
    }

    /**
     * Stops all features based on ProtocolLib.
     */
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

    @Override
    public void reload(Settings settings) {
        boolean oldProtectInventory = this.protectInvBeforeLogin;

        this.protectInvBeforeLogin = settings.getProperty(RestrictionSettings.PROTECT_INVENTORY_BEFORE_LOGIN);
        this.denyTabCompleteBeforeLogin = settings.getProperty(RestrictionSettings.DENY_TABCOMPLETE_BEFORE_LOGIN);

        //it was true and will be deactivated now, so we need to restore the inventory for every player
        if (oldProtectInventory && !protectInvBeforeLogin && inventoryPacketAdapter != null) {
            inventoryPacketAdapter.unregister();
            for (Player onlinePlayer : bukkitService.getOnlinePlayers()) {
                if (!playerCache.isAuthenticated(onlinePlayer.getName())) {
                    onlinePlayer.updateInventory();
                }
            }
        }
        setup();
    }

}
