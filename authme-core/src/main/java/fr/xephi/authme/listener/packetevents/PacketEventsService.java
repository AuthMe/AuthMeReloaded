package fr.xephi.authme.listener.packetevents;

import ch.jalu.injector.annotations.NoFieldScan;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.platform.PacketInterceptionAdapter;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.PendingPremiumCache;
import fr.xephi.authme.service.PremiumLoginVerifier;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.PremiumSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.inject.Inject;

@NoFieldScan
public class PacketEventsService implements SettingsDependent {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(PacketEventsService.class);

    private boolean protectInvBeforeLogin;
    private boolean denyTabCompleteBeforeLogin;
    private boolean enablePremium;
    private boolean bungeecordEnabled;
    private boolean inventoryProtectionRegistered;
    private boolean tabCompleteBlockRegistered;
    private boolean premiumVerificationRegistered;

    private final BukkitService bukkitService;
    private final PlayerCache playerCache;
    private final DataSource dataSource;
    private final PacketInterceptionAdapter packetInterceptionAdapter;
    private final PremiumLoginVerifier premiumLoginVerifier;
    private final PendingPremiumCache pendingPremiumCache;

    @Inject
    PacketEventsService(Settings settings, BukkitService bukkitService, PlayerCache playerCache,
                        DataSource dataSource, PacketInterceptionAdapter packetInterceptionAdapter,
                        PremiumLoginVerifier premiumLoginVerifier, PendingPremiumCache pendingPremiumCache) {
        this.bukkitService = bukkitService;
        this.playerCache = playerCache;
        this.dataSource = dataSource;
        this.packetInterceptionAdapter = packetInterceptionAdapter;
        this.premiumLoginVerifier = premiumLoginVerifier;
        this.pendingPremiumCache = pendingPremiumCache;
        reload(settings);
    }

    /**
     * Sets up the PacketEvents packet listeners.
     */
    public void setup() {
        boolean isProxyMode = bungeecordEnabled || packetInterceptionAdapter.isProxyForwardingEnabled();
        boolean needsPremiumPacketVerification = enablePremium
            && !Bukkit.getServer().getOnlineMode()
            && !isProxyMode;

        boolean packetEventsAvailable = Bukkit.getPluginManager().isPluginEnabled("packetevents");
        if (!packetEventsAvailable) {
            if (protectInvBeforeLogin) {
                logger.warning("WARNING! The protectInventory feature requires PacketEvents! Disabling it...");
            }
            if (denyTabCompleteBeforeLogin) {
                logger.warning("WARNING! The denyTabComplete feature requires PacketEvents! Disabling it...");
            }
            if (needsPremiumPacketVerification) {
                logger.warning("WARNING! Premium bypass requires the PacketEvents plugin for session "
                    + "verification! Premium auto-login is disabled until PacketEvents is installed.");
            }
            return;
        }

        if (protectInvBeforeLogin) {
            if (!inventoryProtectionRegistered) {
                packetInterceptionAdapter.registerInventoryProtection(playerCache, dataSource);
                inventoryProtectionRegistered = true;
                // Send blank packets to online unauthenticated players (reload scenario)
                for (Player player : bukkitService.getOnlinePlayers()) {
                    if (!playerCache.isAuthenticated(player.getName())
                            && dataSource.isAuthAvailable(player.getName())) {
                        packetInterceptionAdapter.sendBlankInventoryPacket(player);
                    }
                }
            }
        } else if (inventoryProtectionRegistered) {
            packetInterceptionAdapter.unregisterInventoryProtection();
            inventoryProtectionRegistered = false;
        }

        if (denyTabCompleteBeforeLogin) {
            if (!tabCompleteBlockRegistered) {
                packetInterceptionAdapter.registerTabCompleteBlock(playerCache);
                tabCompleteBlockRegistered = true;
            }
        } else if (tabCompleteBlockRegistered) {
            packetInterceptionAdapter.unregisterTabCompleteBlock();
            tabCompleteBlockRegistered = false;
        }

        if (needsPremiumPacketVerification) {
            if (!premiumVerificationRegistered) {
                packetInterceptionAdapter.registerPremiumVerification(dataSource, premiumLoginVerifier,
                    pendingPremiumCache);
                premiumVerificationRegistered = true;
            }
        } else if (premiumVerificationRegistered) {
            packetInterceptionAdapter.unregisterPremiumVerification();
            premiumVerificationRegistered = false;
        }
    }

    /**
     * Stops all PacketEvents-based features.
     */
    public void disable() {
        if (inventoryProtectionRegistered) {
            packetInterceptionAdapter.unregisterInventoryProtection();
            inventoryProtectionRegistered = false;
        }
        if (tabCompleteBlockRegistered) {
            packetInterceptionAdapter.unregisterTabCompleteBlock();
            tabCompleteBlockRegistered = false;
        }
        if (premiumVerificationRegistered) {
            packetInterceptionAdapter.unregisterPremiumVerification();
            premiumVerificationRegistered = false;
        }
    }

    /**
     * Sends a blank inventory packet to the given player.
     *
     * @param player the player to send the blank inventory to
     */
    public void sendBlankInventoryPacket(Player player) {
        if (inventoryProtectionRegistered) {
            packetInterceptionAdapter.sendBlankInventoryPacket(player);
        }
    }

    @Override
    public void reload(Settings settings) {
        boolean oldProtectInventory = this.protectInvBeforeLogin;

        this.protectInvBeforeLogin = settings.getProperty(RestrictionSettings.PROTECT_INVENTORY_BEFORE_LOGIN);
        this.denyTabCompleteBeforeLogin = settings.getProperty(RestrictionSettings.DENY_TABCOMPLETE_BEFORE_LOGIN);
        this.enablePremium = settings.getProperty(PremiumSettings.ENABLE_PREMIUM);
        this.bungeecordEnabled = settings.getProperty(HooksSettings.BUNGEECORD);

        // If inventory protection was on and is now disabled, restore inventories for online players
        if (oldProtectInventory && !protectInvBeforeLogin && inventoryProtectionRegistered) {
            packetInterceptionAdapter.unregisterInventoryProtection();
            inventoryProtectionRegistered = false;
            for (Player onlinePlayer : bukkitService.getOnlinePlayers()) {
                if (!playerCache.isAuthenticated(onlinePlayer.getName())) {
                    onlinePlayer.updateInventory();
                }
            }
        }
        setup();
    }
}
