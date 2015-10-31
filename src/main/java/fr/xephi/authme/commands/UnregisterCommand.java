package fr.xephi.authme.commands;

import java.security.NoSuchAlgorithmException;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

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

public class UnregisterCommand implements CommandExecutor {

    private Messages m = Messages.getInstance();
    public AuthMe plugin;
    private JsonCache playerCache;

    public UnregisterCommand(AuthMe plugin) {
        this.plugin = plugin;
        this.playerCache = new JsonCache();
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

        plugin.management.performUnregister(player, args[0], false);
        return true;
    }
}
