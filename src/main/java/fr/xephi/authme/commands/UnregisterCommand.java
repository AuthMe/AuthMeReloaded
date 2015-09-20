package fr.xephi.authme.commands;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.Utils;
import fr.xephi.authme.Utils.GroupType;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.backup.JsonCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.events.SpawnTeleportEvent;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.security.NoSuchAlgorithmException;

public class UnregisterCommand implements CommandExecutor {

    private Messages m = Messages.getInstance();
    public AuthMe plugin;
    private JsonCache playerCache;

    public UnregisterCommand(AuthMe plugin) {
        this.plugin = plugin;
        this.playerCache = new JsonCache(plugin);
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command cmnd, String label,
                             final String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        if (!plugin.authmePermissible(sender, "authme." + label.toLowerCase())) {
            m.send(sender, "no_perm");
            return true;
        }

        final Player player = (Player) sender;
        final String name = player.getName().toLowerCase();

        if (!PlayerCache.getInstance().isAuthenticated(name)) {
            m.send(player, "not_logged_in");
            return true;
        }

        if (args.length != 1) {
            m.send(player, "usage_unreg");
            return true;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    if (PasswordSecurity.comparePasswordWithHash(args[0], PlayerCache.getInstance().getAuth(name).getHash(), player.getName())) {
                        if (!plugin.database.removeAuth(name)) {
                            player.sendMessage("error");
                            return;
                        }
                        if (Settings.isForcedRegistrationEnabled) {
                            if (Settings.isTeleportToSpawnEnabled && !Settings.noTeleport) {
                                Location spawn = plugin.getSpawnLocation(player);
                                SpawnTeleportEvent tpEvent = new SpawnTeleportEvent(player, player.getLocation(), spawn, false);
                                plugin.getServer().getPluginManager().callEvent(tpEvent);
                                if (!tpEvent.isCancelled()) {
                                    player.teleport(tpEvent.getTo());
                                }
                            }
                            player.getInventory().setContents(new ItemStack[36]);
                            player.getInventory().setArmorContents(new ItemStack[4]);
                            player.saveData();
                            PlayerCache.getInstance().removePlayer(player.getName().toLowerCase());
                            if (!Settings.getRegisteredGroup.isEmpty())
                                Utils.setGroup(player, GroupType.UNREGISTERED);
                            LimboCache.getInstance().addLimboPlayer(player);
                            int delay = Settings.getRegistrationTimeout * 20;
                            int interval = Settings.getWarnMessageInterval;
                            BukkitScheduler sched = sender.getServer().getScheduler();
                            if (delay != 0) {
                                BukkitTask id = sched.runTaskLaterAsynchronously(plugin, new TimeoutTask(plugin, name, player), delay);
                                LimboCache.getInstance().getLimboPlayer(name).setTimeoutTaskId(id);
                            }
                            LimboCache.getInstance().getLimboPlayer(name).setMessageTaskId(sched.runTaskAsynchronously(plugin, new MessageTask(plugin, name, m.send("reg_msg"), interval)));
                            m.send(player, "unregistered");
                            ConsoleLogger.info(player.getDisplayName() + " unregistered himself");
                            return;
                        }
                        if (!Settings.unRegisteredGroup.isEmpty()) {
                            Utils.setGroup(player, Utils.GroupType.UNREGISTERED);
                        }
                        PlayerCache.getInstance().removePlayer(player.getName().toLowerCase());
                        // check if Player cache File Exist and delete it, preventing
                        // duplication of items
                        if (playerCache.doesCacheExist(player)) {
                            playerCache.removeCache(player);
                        }
                        if (Settings.applyBlindEffect)
                            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Settings.getRegistrationTimeout * 20, 2));
                        if (!Settings.isMovementAllowed && Settings.isRemoveSpeedEnabled) {
                            player.setWalkSpeed(0.0f);
                            player.setFlySpeed(0.0f);
                        }
                        m.send(player, "unregistered");
                        ConsoleLogger.info(player.getDisplayName() + " unregistered himself");
                        if (Settings.isTeleportToSpawnEnabled && !Settings.noTeleport) {
                            Location spawn = plugin.getSpawnLocation(player);
                            SpawnTeleportEvent tpEvent = new SpawnTeleportEvent(player, player.getLocation(), spawn, false);
                            plugin.getServer().getPluginManager().callEvent(tpEvent);
                            if (!tpEvent.isCancelled()) {
                                if (!tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).isLoaded()) {
                                    tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).load();
                                }
                                player.teleport(tpEvent.getTo());
                            }
                        }
                        return;
                    } else {
                        m.send(player, "wrong_pwd");
                    }
                } catch (NoSuchAlgorithmException ex) {
                    ConsoleLogger.showError(ex.getMessage());
                    sender.sendMessage("Internal Error please read the server log");
                }
            }
        });
        return true;
    }
}
