package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.task.purge.PurgeExecutor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.List;
import java.util.Locale;

import static java.util.Collections.singletonList;

/**
 * Command to purge a player.
 */
public class PurgePlayerCommand implements ExecutableCommand {

    @Inject
    private PurgeExecutor purgeExecutor;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private DataSource dataSource;

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments) {
        String option = arguments.size() > 1 ? arguments.get(1) : null;
        bukkitService.runOnAsyncSchedulerNow(task -> executeCommand(sender, arguments.get(0), option));
    }

    private void executeCommand(CommandSender sender, String name, String option) {
        if ("force".equals(option) || !dataSource.isAuthAvailable(name)) {
            OfflinePlayer offlinePlayer = bukkitService.getOfflinePlayer(name);
            purgeExecutor.executePurge(singletonList(offlinePlayer), singletonList(name.toLowerCase(Locale.ROOT)));
            sender.sendMessage("Purged data for player " + name);
        } else {
            sender.sendMessage("This player is still registered! Are you sure you want to proceed? "
                + "Use '/authme purgeplayer " + name + " force' to run the command anyway");
        }
    }
}
