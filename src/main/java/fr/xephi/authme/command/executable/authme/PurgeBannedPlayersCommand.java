package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.task.PurgeTask;
import fr.xephi.authme.util.BukkitService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import org.bukkit.ChatColor;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

/**
 * Command for purging data of banned players. Depending on the settings
 * it purges (deletes) data from third-party plugins as well.
 */
public class PurgeBannedPlayersCommand implements ExecutableCommand {

    @Inject
    private DataSource dataSource;

    @Inject
    private AuthMe plugin;

    @Inject
    private BukkitService bukkitService;

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments, CommandService commandService) {
        // Get the list of banned players
        Set<String> namedBanned = new HashSet<>();
        Set<OfflinePlayer> bannedPlayers = bukkitService.getBannedPlayers();
        for (OfflinePlayer offlinePlayer : bannedPlayers) {
            namedBanned.add(offlinePlayer.getName().toLowerCase());
        }

        //todo: note this should may run async because it may executes a SQL-Query
        // Purge the banned players
        dataSource.purgeBanned(namedBanned);

        // Show a status message
        sender.sendMessage(ChatColor.GOLD + "Purging user accounts...");
        new PurgeTask(plugin, sender, namedBanned, bannedPlayers).runTaskTimer(plugin, 0, 1);
    }
}
