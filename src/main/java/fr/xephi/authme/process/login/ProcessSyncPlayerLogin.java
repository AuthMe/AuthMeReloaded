package fr.xephi.authme.process.login;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.limbo.LimboPlayer;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.events.RestoreInventoryEvent;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.process.SynchronousProcess;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.JoinMessageService;
import fr.xephi.authme.service.TeleportationService;
import fr.xephi.authme.service.bungeecord.BungeeSender;
import fr.xephi.authme.settings.WelcomeMessageConfiguration;
import fr.xephi.authme.settings.commandconfig.CommandManager;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import javax.inject.Inject;
import java.util.List;
import java.util.Locale;

import static fr.xephi.authme.settings.properties.RestrictionSettings.PROTECT_INVENTORY_BEFORE_LOGIN;

public class ProcessSyncPlayerLogin implements SynchronousProcess {

    @Inject
    private BungeeSender bungeeSender;

    @Inject
    private LimboService limboService;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private TeleportationService teleportationService;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private CommandManager commandManager;

    @Inject
    private CommonService commonService;

    @Inject
    private WelcomeMessageConfiguration welcomeMessageConfiguration;

    @Inject
    private JoinMessageService joinMessageService;

    @Inject
    private PermissionsManager permissionsManager;

    ProcessSyncPlayerLogin() {
    }

    private void restoreInventory(Player player) {
        RestoreInventoryEvent event = new RestoreInventoryEvent(player);
        bukkitService.callEvent(event);
        if (!event.isCancelled()) {
            player.updateInventory();
        }
    }

    /**
     * Performs operations in sync mode for a player that has just logged in.
     *
     * @param player the player that was logged in
     * @param isFirstLogin true if this is the first time the player logged in
     * @param authsWithSameIp registered names with the same IP address as the player's
     */
    public void processPlayerLogin(Player player, boolean isFirstLogin, List<String> authsWithSameIp) {
        final String name = player.getName().toLowerCase(Locale.ROOT);
        final LimboPlayer limbo = limboService.getLimboPlayer(name);

        // Limbo contains the State of the Player before /login
        if (limbo != null) {
            limboService.restoreData(player);
        }

        if (commonService.getProperty(PROTECT_INVENTORY_BEFORE_LOGIN)) {
            restoreInventory(player);
        }

        final PlayerAuth auth = playerCache.getAuth(name);
        teleportationService.teleportOnLogin(player, auth, limbo);

        // We can now display the join message (if delayed)
        joinMessageService.sendMessage(name);

        if (commonService.getProperty(RegistrationSettings.APPLY_BLIND_EFFECT)) {
            player.removePotionEffect(PotionEffectType.BLINDNESS);
        }

        // The Login event now fires (as intended) after everything is processed
        bukkitService.callEvent(new LoginEvent(player));

        // Login is done, display welcome message
        welcomeMessageConfiguration.sendWelcomeMessage(player);

        // Login is now finished; we can force all commands
        if (isFirstLogin) {
            commandManager.runCommandsOnFirstLogin(player, authsWithSameIp);
        }
        commandManager.runCommandsOnLogin(player, authsWithSameIp);

        if (!permissionsManager.hasPermission(player, PlayerStatePermission.BYPASS_BUNGEE_SEND)) {
            // Send Bungee stuff. The service will check if it is enabled or not.
            bungeeSender.connectPlayerOnLogin(player);
        }
    }
}
