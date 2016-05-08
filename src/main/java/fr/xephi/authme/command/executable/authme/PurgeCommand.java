package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.hooks.PluginHooks;
import fr.xephi.authme.settings.properties.PurgeSettings;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.Calendar;
import java.util.List;

/**
 * Command for purging the data of players which have not been since for a given number
 * of days. Depending on the settings, this removes player data in third-party plugins as well.
 */
public class PurgeCommand implements ExecutableCommand {

    private static final int MINIMUM_LAST_SEEN_DAYS = 30;

    @Inject
    private DataSource dataSource;

    @Inject
    private PluginHooks pluginHooks;

    @Inject
    private AuthMe plugin;

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments, CommandService commandService) {
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

        // Purge the data, get the purged values
        List<String> purged = dataSource.autoPurgeDatabase(until);

        // Show a status message
        sender.sendMessage(ChatColor.GOLD + "Deleted " + purged.size() + " user accounts");

        // Purge other data
        if (commandService.getProperty(PurgeSettings.REMOVE_ESSENTIALS_FILES)
            && pluginHooks.isEssentialsAvailable())
            plugin.dataManager.purgeEssentials(purged);
        if (commandService.getProperty(PurgeSettings.REMOVE_PLAYER_DAT))
            plugin.dataManager.purgeDat(purged);
        if (commandService.getProperty(PurgeSettings.REMOVE_LIMITED_CREATIVE_INVENTORIES))
            plugin.dataManager.purgeLimitedCreative(purged);
        if (commandService.getProperty(PurgeSettings.REMOVE_ANTI_XRAY_FILE))
            plugin.dataManager.purgeAntiXray(purged);

        // Show a status message
        sender.sendMessage(ChatColor.GREEN + "[AuthMe] Database has been purged correctly");
    }
}
