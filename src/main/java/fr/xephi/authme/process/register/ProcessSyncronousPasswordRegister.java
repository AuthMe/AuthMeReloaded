package fr.xephi.authme.process.register;

import me.muizers.Notifications.Notification;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.Utils;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.events.AuthMeTeleportEvent;
import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.events.RegisterTeleportEvent;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;

public class ProcessSyncronousPasswordRegister implements Runnable {

    protected Player player;
    protected String name;
    private AuthMe plugin;
    private Messages m = Messages.getInstance();

    public ProcessSyncronousPasswordRegister(Player player, AuthMe plugin) {
        this.player = player;
        this.name = player.getName();
        this.plugin = plugin;
    }

    protected void forceCommands() {
        for (String command : Settings.forceRegisterCommands) {
            try {
                player.performCommand(command.replace("%p", player.getName()));
            } catch (Exception e) {
            }
        }
        for (String command : Settings.forceRegisterCommandsAsConsole) {
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command.replace("%p", player.getName()));
        }
    }

    protected void forceLogin(Player player) {
        if (Settings.isTeleportToSpawnEnabled && !Settings.noTeleport) {
            Location spawnLoc = plugin.getSpawnLocation(player);
            AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(player, spawnLoc);
            plugin.getServer().getPluginManager().callEvent(tpEvent);
            if (!tpEvent.isCancelled()) {
                if (!tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).isLoaded()) {
                    tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).load();
                }
                player.teleport(tpEvent.getTo());
            }
        }
        if (LimboCache.getInstance().hasLimboPlayer(name))
            LimboCache.getInstance().deleteLimboPlayer(name);
        LimboCache.getInstance().addLimboPlayer(player);
        int delay = Settings.getRegistrationTimeout * 20;
        int interval = Settings.getWarnMessageInterval;
        BukkitScheduler sched = plugin.getServer().getScheduler();
        if (delay != 0) {
            int id = sched.scheduleSyncDelayedTask(plugin, new TimeoutTask(plugin, name), delay);
            LimboCache.getInstance().getLimboPlayer(name).setTimeoutTaskId(id);
        }
        int msgT = sched.scheduleSyncDelayedTask(plugin, new MessageTask(plugin, name, m._("login_msg"), interval));
        LimboCache.getInstance().getLimboPlayer(name).setMessageTaskId(msgT);
        try {
            plugin.pllog.removePlayer(name);
            if (player.isInsideVehicle())
                player.getVehicle().eject();
        } catch (NullPointerException npe) {
        }
    }

    @Override
    public void run() {
        LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(name);
        if (limbo != null) {
            player.setGameMode(limbo.getGameMode());
            if (Settings.isTeleportToSpawnEnabled && !Settings.noTeleport) {
                Location loca = plugin.getSpawnLocation(player);
                RegisterTeleportEvent tpEvent = new RegisterTeleportEvent(player, loca);
                plugin.getServer().getPluginManager().callEvent(tpEvent);
                if (!tpEvent.isCancelled()) {
                    if (!tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).isLoaded()) {
                        tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).load();
                    }
                    player.teleport(tpEvent.getTo());
                }
            }
            plugin.getServer().getScheduler().cancelTask(limbo.getTimeoutTaskId());
            plugin.getServer().getScheduler().cancelTask(limbo.getMessageTaskId());
            LimboCache.getInstance().deleteLimboPlayer(name);
        }

        if (!Settings.getRegisteredGroup.isEmpty()) {
            Utils.getInstance().setGroup(player, Utils.groupType.REGISTERED);
        }
        m._(player, "registered");
        if (!Settings.getmailAccount.isEmpty())
            m._(player, "add_email");
        if (player.getGameMode() != GameMode.CREATIVE && !Settings.isMovementAllowed) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
        if (Settings.applyBlindEffect)
            player.removePotionEffect(PotionEffectType.BLINDNESS);
        // The Loginevent now fires (as intended) after everything is processed
        Bukkit.getServer().getPluginManager().callEvent(new LoginEvent(player, true));
        player.saveData();

        if (!Settings.noConsoleSpam)
            ConsoleLogger.info(player.getName() + " registered " + plugin.getIP(player));
        if (plugin.notifications != null) {
            plugin.notifications.showNotification(new Notification("[AuthMe] " + player.getName() + " has registered!"));
        }

        // Kick Player after Registration is enabled, kick the player
        if (Settings.forceRegKick) {
            player.kickPlayer(m._("registered")[0]);
            return;
        }

        // Request Login after Registation
        if (Settings.forceRegLogin) {
            forceLogin(player);
            return;
        }

        // Register is finish and player is logged, display welcome message
        if (Settings.useWelcomeMessage)
            if (Settings.broadcastWelcomeMessage) {
                for (String s : Settings.welcomeMsg) {
                    Bukkit.getServer().broadcastMessage(plugin.replaceAllInfos(s, player));
                }
            } else {
                for (String s : Settings.welcomeMsg) {
                    player.sendMessage(plugin.replaceAllInfos(s, player));
                }
            }

        // Register is now finish , we can force all commands
        forceCommands();

    }
}
