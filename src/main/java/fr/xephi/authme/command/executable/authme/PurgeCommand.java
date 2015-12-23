package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.settings.Settings;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Calendar;
import java.util.List;

public class PurgeCommand implements ExecutableCommand {

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments, CommandService commandService) {
        // AuthMe plugin instance
        AuthMe plugin = AuthMe.getInstance();

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
        if (days < 30) {
            sender.sendMessage(ChatColor.RED + "You can only purge data older than 30 days");
            return;
        }

        // Create a calender instance to determine the date
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -days);
        long until = calendar.getTimeInMillis();

        // Purge the data, get the purged values
        List<String> purged = plugin.database.autoPurgeDatabase(until);

        // Show a status message
        sender.sendMessage(ChatColor.GOLD + "Deleted " + purged.size() + " user accounts");

        // Purge other data
        if (Settings.purgeEssentialsFile && plugin.ess != null)
            plugin.dataManager.purgeEssentials(purged);
        if (Settings.purgePlayerDat)
            plugin.dataManager.purgeDat(purged);
        if (Settings.purgeLimitedCreative)
            plugin.dataManager.purgeLimitedCreative(purged);
        if (Settings.purgeAntiXray)
            plugin.dataManager.purgeAntiXray(purged);

        // Show a status message
        sender.sendMessage(ChatColor.GREEN + "[AuthMe] Database has been purged correctly");
    }
}
