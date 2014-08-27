package fr.xephi.authme.commands;

import java.security.NoSuchAlgorithmException;

import me.muizers.Notifications.Notification;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.Utils;
import fr.xephi.authme.Utils.groupType;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.backup.FileCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.SpawnTeleportEvent;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;

public class UnregisterCommand implements CommandExecutor {

    private Messages m = Messages.getInstance();
    public AuthMe plugin;
    private DataSource database;
    private FileCache playerCache;

    public UnregisterCommand(AuthMe plugin, DataSource database) {
        this.plugin = plugin;
        this.database = database;
        this.playerCache = new FileCache(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmnd, String label,
            String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        if (!plugin.authmePermissible(sender, "authme." + label.toLowerCase())) {
            m._(sender, "no_perm");
            return true;
        }

        Player player = (Player) sender;
        String name = player.getName();

        if (!PlayerCache.getInstance().isAuthenticated(name)) {
            m._(player, "not_logged_in");
            return true;
        }

        if (args.length != 1) {
            m._(player, "usage_unreg");
            return true;
        }
        try {
            if (PasswordSecurity.comparePasswordWithHash(args[0], PlayerCache.getInstance().getAuth(name).getHash(), player.getName())) {
                if (!database.removeAuth(name)) {
                    player.sendMessage("error");
                    return true;
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
                    PlayerCache.getInstance().removePlayer(player.getName());
                    if (!Settings.getRegisteredGroup.isEmpty())
                        Utils.getInstance().setGroup(player, groupType.UNREGISTERED);
                    LimboCache.getInstance().addLimboPlayer(player);
                    int delay = Settings.getRegistrationTimeout * 20;
                    int interval = Settings.getWarnMessageInterval;
                    BukkitScheduler sched = sender.getServer().getScheduler();
                    if (delay != 0) {
                        int id = sched.scheduleSyncDelayedTask(plugin, new TimeoutTask(plugin, name), delay);
                        LimboCache.getInstance().getLimboPlayer(name).setTimeoutTaskId(id);
                    }
                    LimboCache.getInstance().getLimboPlayer(name).setMessageTaskId(sched.scheduleSyncDelayedTask(plugin, new MessageTask(plugin, name, m._("reg_msg"), interval)));
                    m._(player, "unregistered");
                    ConsoleLogger.info(player.getDisplayName() + " unregistered himself");
                    if (plugin.notifications != null) {
                        plugin.notifications.showNotification(new Notification("[AuthMe] " + player.getName() + " unregistered himself!"));
                    }
                    return true;
                }
                if (!Settings.unRegisteredGroup.isEmpty()) {
                    Utils.getInstance().setGroup(player, Utils.groupType.UNREGISTERED);
                }
                PlayerCache.getInstance().removePlayer(player.getName());
                // check if Player cache File Exist and delete it, preventing
                // duplication of items
                if (playerCache.doesCacheExist(player)) {
                    playerCache.removeCache(player);
                }
                if (Settings.applyBlindEffect)
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Settings.getRegistrationTimeout * 20, 2));
                m._(player, "unregistered");
                ConsoleLogger.info(player.getDisplayName() + " unregistered himself");
                if (plugin.notifications != null) {
                    plugin.notifications.showNotification(new Notification("[AuthMe] " + player.getName() + " unregistered himself!"));
                }
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
                return true;
            } else {
                m._(player, "wrong_pwd");
            }
        } catch (NoSuchAlgorithmException ex) {
            ConsoleLogger.showError(ex.getMessage());
            sender.sendMessage("Internal Error please read the server log");
        }
        return true;
    }
}
