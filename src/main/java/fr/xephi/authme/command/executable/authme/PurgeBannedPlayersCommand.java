package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.settings.properties.PurgeSettings;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class PurgeBannedPlayersCommand implements ExecutableCommand {

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments, CommandService commandService) {
        // AuthMe plugin instance
        final AuthMe plugin = commandService.getAuthMe();

        // Get the list of banned players
        List<String> bannedPlayers = new ArrayList<>();
        for (OfflinePlayer offlinePlayer : plugin.getServer().getBannedPlayers()) {
            bannedPlayers.add(offlinePlayer.getName().toLowerCase());
        }

        // Purge the banned players
        plugin.getDataSource().purgeBanned(bannedPlayers);
        if (commandService.getProperty(PurgeSettings.REMOVE_ESSENTIALS_FILES)
            && commandService.getPluginHooks().isEssentialsAvailable())
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
