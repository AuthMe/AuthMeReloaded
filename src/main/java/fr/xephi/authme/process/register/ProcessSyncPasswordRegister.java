package fr.xephi.authme.process.register;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.events.RestoreInventoryEvent;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;
import fr.xephi.authme.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

/**
 */
public class ProcessSyncPasswordRegister implements Runnable {

    protected final Player player;
    protected final String name;
    private final AuthMe plugin;
    private final Messages m;

    /**
     * Constructor for ProcessSyncPasswordRegister.
     *
     * @param player Player
     * @param plugin AuthMe
     */
    public ProcessSyncPasswordRegister(Player player, AuthMe plugin) {
        this.m = plugin.getMessages();
        this.player = player;
        this.name = player.getName().toLowerCase();
        this.plugin = plugin;
    }

    private void sendBungeeMessage() {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF("AuthMe");
        out.writeUTF("register;" + name);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    private void forceCommands() {
        for (String command : Settings.forceRegisterCommands) {
            player.performCommand(command.replace("%p", player.getName()));
        }
        for (String command : Settings.forceRegisterCommandsAsConsole) {
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
                command.replace("%p", player.getName()));
        }
    }

    /**
     * Method forceLogin.
     *
     * @param player Player
     */
    private void forceLogin(Player player) {
        Utils.teleportToSpawn(player);
        LimboCache cache = LimboCache.getInstance();
        cache.updateLimboPlayer(player);
        int delay = Settings.getRegistrationTimeout * 20;
        int interval = Settings.getWarnMessageInterval;
        BukkitScheduler sched = plugin.getServer().getScheduler();
        BukkitTask task;
        if (delay != 0) {
            task = sched.runTaskLaterAsynchronously(plugin, new TimeoutTask(plugin, name, player), delay);
            cache.getLimboPlayer(name).setTimeoutTaskId(task);
        }
        task = sched.runTaskAsynchronously(plugin, new MessageTask(plugin, name,
            m.retrieve(MessageKey.LOGIN_MESSAGE), interval));
        cache.getLimboPlayer(name).setMessageTaskId(task);
        if (player.isInsideVehicle() && player.getVehicle() != null) {
            player.getVehicle().eject();
        }
    }

    /**
     * Method run.
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(name);
        if (limbo != null) {
            player.setGameMode(limbo.getGameMode());
            Utils.teleportToSpawn(player);

            if (Settings.protectInventoryBeforeLogInEnabled && plugin.inventoryProtector != null) {
                RestoreInventoryEvent event = new RestoreInventoryEvent(player);
                Bukkit.getPluginManager().callEvent(event);
                if (!event.isCancelled()) {
                    plugin.inventoryProtector.sendInventoryPacket(player);
                }
            }

            LimboCache.getInstance().deleteLimboPlayer(name);
        }

        if (!Settings.getRegisteredGroup.isEmpty()) {
            Utils.setGroup(player, Utils.GroupType.REGISTERED);
        }

        m.send(player, MessageKey.REGISTER_SUCCESS);

        if (!Settings.getmailAccount.isEmpty()) {
            m.send(player, MessageKey.ADD_EMAIL_MESSAGE);
        }
        if (player.getGameMode() != GameMode.CREATIVE && !Settings.isMovementAllowed) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }

        if (Settings.applyBlindEffect) {
            player.removePotionEffect(PotionEffectType.BLINDNESS);
        }

        // The LoginEvent now fires (as intended) after everything is processed
        plugin.getServer().getPluginManager().callEvent(new LoginEvent(player, true));
        player.saveData();

        if (!Settings.noConsoleSpam) {
            ConsoleLogger.info(player.getName() + " registered " + plugin.getIP(player));
        }

        // Kick Player after Registration is enabled, kick the player
        if (Settings.forceRegKick) {
            player.kickPlayer(m.retrieveSingle(MessageKey.REGISTER_SUCCESS));
            return;
        }

        // Request Login after Registration
        if (Settings.forceRegLogin) {
            forceLogin(player);
            return;
        }

        // Register is finish and player is logged, display welcome message
        if (Settings.useWelcomeMessage) {
            if (Settings.broadcastWelcomeMessage) {
                for (String s : Settings.welcomeMsg) {
                    plugin.getServer().broadcastMessage(plugin.replaceAllInfo(s, player));
                }
            } else {
                for (String s : Settings.welcomeMsg) {
                    player.sendMessage(plugin.replaceAllInfo(s, player));
                }
            }
        }

        if (Settings.bungee) {
            sendBungeeMessage();
        }

        // Register is now finish , we can force all commands
        forceCommands();
    }
}
