package fr.xephi.authme.process.login;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.limbo.LimboCache;
import fr.xephi.authme.data.limbo.LimboPlayer;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.events.RestoreInventoryEvent;
import fr.xephi.authme.listener.PlayerListener;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.process.SynchronousProcess;
import fr.xephi.authme.service.BungeeService;
import fr.xephi.authme.settings.commandconfig.CommandManager;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.TeleportationService;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.potion.PotionEffectType;

import javax.inject.Inject;

import static fr.xephi.authme.settings.properties.RestrictionSettings.PROTECT_INVENTORY_BEFORE_LOGIN;

public class ProcessSyncPlayerLogin implements SynchronousProcess {

    @Inject
    private AuthMe plugin;

    @Inject
    private BungeeService bungeeService;

    @Inject
    private ProcessService service;

    @Inject
    private LimboCache limboCache;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private PluginManager pluginManager;

    @Inject
    private TeleportationService teleportationService;

    @Inject
    private DataSource dataSource;

    @Inject
    private CommandManager commandManager;

    ProcessSyncPlayerLogin() {
    }

    private void restoreInventory(Player player) {
        RestoreInventoryEvent event = new RestoreInventoryEvent(player);
        pluginManager.callEvent(event);
        if (!event.isCancelled()) {
            player.updateInventory();
        }
    }

    public void processPlayerLogin(Player player) {
        final String name = player.getName().toLowerCase();

        final LimboPlayer limbo = limboCache.getPlayerData(name);
        // Limbo contains the State of the Player before /login
        if (limbo != null) {
            limboCache.restoreData(player);
            limboCache.deletePlayerData(player);
            // do we really need to use location from database for now?
            // because LimboCache#restoreData teleport player to last location.
        }

        if (service.getProperty(PROTECT_INVENTORY_BEFORE_LOGIN)) {
            restoreInventory(player);
        }

        final PlayerAuth auth = dataSource.getAuth(name);
        teleportationService.teleportOnLogin(player, auth, limbo);

        // We can now display the join message (if delayed)
        String joinMessage = PlayerListener.joinMessage.remove(name);
        if (!StringUtils.isEmpty(joinMessage)) {
            for (Player p : bukkitService.getOnlinePlayers()) {
                if (p.isOnline()) {
                    p.sendMessage(joinMessage);
                }
            }
        }

        if (service.getProperty(RegistrationSettings.APPLY_BLIND_EFFECT)) {
            player.removePotionEffect(PotionEffectType.BLINDNESS);
        }

        // The Login event now fires (as intended) after everything is processed
        bukkitService.callEvent(new LoginEvent(player));
        player.saveData();

        // Login is done, display welcome message
        if (service.getProperty(RegistrationSettings.USE_WELCOME_MESSAGE)) {
            if (service.getProperty(RegistrationSettings.BROADCAST_WELCOME_MESSAGE)) {
                for (String s : service.getSettings().getWelcomeMessage()) {
                    Bukkit.getServer().broadcastMessage(plugin.replaceAllInfo(s, player));
                }
            } else {
                for (String s : service.getSettings().getWelcomeMessage()) {
                    player.sendMessage(plugin.replaceAllInfo(s, player));
                }
            }
        }

        // Login is now finished; we can force all commands
        commandManager.runCommandsOnLogin(player);

        // Send Bungee stuff. The service will check if it is enabled or not.
        bungeeService.connectPlayer(player);
    }
}
