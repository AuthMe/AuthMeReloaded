package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.hooks.PluginHooks;
import fr.xephi.authme.settings.properties.PurgeSettings;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Command for purging data of banned players. Depending on the settings
 * it purges (deletes) data from third-party plugins as well.
 */
public class PurgeBannedPlayersCommand implements ExecutableCommand {

    @Inject
    private DataSource dataSource;

    @Inject
    private PluginHooks pluginHooks;

    @Inject
    private AuthMe plugin;

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments, CommandService commandService) {
        // Get the list of banned players
        List<String> bannedPlayers = new ArrayList<>();
        for (OfflinePlayer offlinePlayer : commandService.getBukkitService().getBannedPlayers()) {
            bannedPlayers.add(offlinePlayer.getName().toLowerCase());
        }

        // Purge the banned players
        dataSource.purgeBanned(bannedPlayers);
        if (commandService.getProperty(PurgeSettings.REMOVE_ESSENTIALS_FILES)
            && pluginHooks.isEssentialsAvailable())
            plugin.dataManager.purgeEssentials(bannedPlayers);
        if (commandService.getProperty(PurgeSettings.REMOVE_PLAYER_DAT))
            plugin.dataManager.purgeDat(bannedPlayers);
        if (commandService.getProperty(PurgeSettings.REMOVE_LIMITED_CREATIVE_INVENTORIES))
            plugin.dataManager.purgeLimitedCreative(bannedPlayers);
        if (commandService.getProperty(PurgeSettings.REMOVE_ANTI_XRAY_FILE))
            plugin.dataManager.purgeAntiXray(bannedPlayers);

        // Show a status message
        sender.sendMessage("[AuthMe] Database has been purged correctly");
    }
}
