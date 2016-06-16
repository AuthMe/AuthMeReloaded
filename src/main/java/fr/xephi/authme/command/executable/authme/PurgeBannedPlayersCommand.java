package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.process.purge.PurgeService;
import fr.xephi.authme.task.PurgeTask;
import fr.xephi.authme.util.BukkitService;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Command for purging data of banned players. Depending on the settings
 * it purges (deletes) data from third-party plugins as well.
 */
public class PurgeBannedPlayersCommand implements ExecutableCommand {

    @Inject
    private PurgeService purgeService;

    @Inject
    private BukkitService bukkitService;

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments) {
        // Get the list of banned players
        Set<String> namedBanned = new HashSet<>();
        Set<OfflinePlayer> bannedPlayers = bukkitService.getBannedPlayers();
        for (OfflinePlayer offlinePlayer : bannedPlayers) {
            namedBanned.add(offlinePlayer.getName().toLowerCase());
        }

        purgeService.purgeBanned(sender, namedBanned, bannedPlayers);
    }
}
