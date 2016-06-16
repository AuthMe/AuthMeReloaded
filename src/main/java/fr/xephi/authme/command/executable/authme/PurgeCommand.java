package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.process.purge.PurgeService;
import fr.xephi.authme.task.PurgeTask;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

/**
 * Command for purging the data of players which have not been since for a given number
 * of days. Depending on the settings, this removes player data in third-party plugins as well.
 */
public class PurgeCommand implements ExecutableCommand {

    private static final int MINIMUM_LAST_SEEN_DAYS = 30;

    @Inject
    private PurgeService purgeService;

    @Inject
    private AuthMe plugin;

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments) {
        // Get the days parameter
        String daysStr = arguments.get(0);

        // Convert the days string to an integer value, and make sure it's valid
        int days;
        try {
            days = Integer.parseInt(daysStr);
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "The value you've entered is invalid!");
            return;
        }

        // Validate the value
        if (days < MINIMUM_LAST_SEEN_DAYS) {
            sender.sendMessage(ChatColor.RED + "You can only purge data older than "
                + MINIMUM_LAST_SEEN_DAYS + " days");
            return;
        }

        // Create a calender instance to determine the date
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -days);
        long until = calendar.getTimeInMillis();

        // Run the purge
        purgeService.runPurge(sender, until);
    }
}
