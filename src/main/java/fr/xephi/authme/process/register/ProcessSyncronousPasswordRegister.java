package fr.xephi.authme.process.register;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.Utils;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.events.AuthMeTeleportEvent;
import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.events.RegisterTeleportEvent;
import fr.xephi.authme.events.RestoreInventoryEvent;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

public class ProcessSyncronousPasswordRegister implements Runnable {

    protected Player player;
    protected String name;
    private AuthMe plugin;
    private Messages m = Messages.getInstance();

    public ProcessSyncronousPasswordRegister(Player player, AuthMe plugin) {
        this.player = player;
        this.name = player.getName().toLowerCase();
        this.plugin = plugin;
    }

    protected void forceCommands() {
        for (String command : Settings.forceRegisterCommands) {
            try {
                player.performCommand(command.replace("%p", player.getName()));
            } catch (Exception ignored) {
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
            BukkitTask id = sched.runTaskLaterAsynchronously(plugin, new TimeoutTask(plugin, name, player), delay);
            LimboCache.getInstance().getLimboPlayer(name).setTimeoutTaskId(id);
        }
        BukkitTask msgT = sched.runTaskAsynchronously(plugin, new MessageTask(plugin, name, m.send("login_msg"), interval));
        LimboCache.getInstance().getLimboPlayer(name).setMessageTaskId(msgT);
        if (player.isInsideVehicle() && player.getVehicle() != null) {
            player.getVehicle().eject();
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
            if (Settings.protectInventoryBeforeLogInEnabled && limbo.getInventory() != null && limbo.getArmour() != null) {
                RestoreInventoryEvent event = new RestoreInventoryEvent(player, limbo.getInventory(), limbo.getArmour());
                Bukkit.getPluginManager().callEvent(event);
                if (!event.isCancelled() && event.getArmor() != null && event.getInventory() != null) {
                    player.getInventory().setContents(event.getInventory());
                    player.getInventory().setArmorContents(event.getArmor());
                }
            }
            limbo.getTimeoutTaskId().cancel();
            limbo.getMessageTaskId().cancel();
            LimboCache.getInstance().deleteLimboPlayer(name);
        }

        if (!Settings.getRegisteredGroup.isEmpty()) {
            Utils.setGroup(player, Utils.GroupType.REGISTERED);
        }
        m.send(player, "registered");
        if (!Settings.getmailAccount.isEmpty())
            m.send(player, "add_email");
        if (player.getGameMode() != GameMode.CREATIVE && !Settings.isMovementAllowed) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
        if (Settings.applyBlindEffect)
            player.removePotionEffect(PotionEffectType.BLINDNESS);
        if (!Settings.isMovementAllowed && Settings.isRemoveSpeedEnabled) {
            player.setWalkSpeed(0.2f);
            player.setFlySpeed(0.1f);
        }
        // The LoginEvent now fires (as intended) after everything is processed
        plugin.getServer().getPluginManager().callEvent(new LoginEvent(player, true));
        player.saveData();

        if (!Settings.noConsoleSpam)
            ConsoleLogger.info(player.getName() + " registered " + plugin.getIP(player));

        // Kick Player after Registration is enabled, kick the player
        if (Settings.forceRegKick) {
            player.kickPlayer(m.send("registered")[0]);
            return;
        }

        // Request Login after Registration
        if (Settings.forceRegLogin) {
            forceLogin(player);
            return;
        }

        // Register is finish and player is logged, display welcome message
        if (Settings.useWelcomeMessage)
            if (Settings.broadcastWelcomeMessage) {
                for (String s : Settings.welcomeMsg) {
                    plugin.getServer().broadcastMessage(plugin.replaceAllInfos(s, player));
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
