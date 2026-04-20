package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.task.purge.PurgeService;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
        Set<OfflinePlayer> bannedPlayers = bukkitService.getBannedPlayers();
        Set<String> namedBanned = new HashSet<>(bannedPlayers.size());
        for (OfflinePlayer offlinePlayer : bannedPlayers) {
            namedBanned.add(offlinePlayer.getName().toLowerCase(Locale.ROOT));
        }

        purgeService.purgePlayers(sender, namedBanned, bannedPlayers.toArray(new OfflinePlayer[bannedPlayers.size()]));
    }
}
