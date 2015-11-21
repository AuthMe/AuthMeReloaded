package fr.xephi.authme.command.executable.authme;

import java.util.Calendar;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.settings.Settings;

/**
 */
public class PurgeCommand extends ExecutableCommand {

    /**
     * Execute the command.
     *
     * @param sender           The command sender.
     * @param commandReference The command reference.
     * @param commandArguments The command arguments.
     *
    
     * @return True if the command was executed successfully, false otherwise. */
    @Override
    public boolean executeCommand(CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        // AuthMe plugin instance
        AuthMe plugin = AuthMe.getInstance();

        // Get the days parameter
        String daysStr = commandArguments.get(0);

        // Convert the days string to an integer value, and make sure it's valid
        int days;
        try {
            days = Integer.valueOf(daysStr);
        } catch(Exception ex) {
            sender.sendMessage(ChatColor.RED + "The value you've entered is invalid!");
            return true;
        }

        // Validate the value
        if(days < 30) {
            sender.sendMessage(ChatColor.RED + "You can only purge data older than 30 days");
            return true;
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
        if(Settings.purgeEssentialsFile && plugin.ess != null)
            plugin.dataManager.purgeEssentials(purged);
        if(Settings.purgePlayerDat)
            plugin.dataManager.purgeDat(purged);
        if(Settings.purgeLimitedCreative)
            plugin.dataManager.purgeLimitedCreative(purged);
        if(Settings.purgeAntiXray)
            plugin.dataManager.purgeAntiXray(purged);

        // Show a status message
        sender.sendMessage(ChatColor.GREEN + "[AuthMe] Database has been purged correctly");
        return true;
    }
}
