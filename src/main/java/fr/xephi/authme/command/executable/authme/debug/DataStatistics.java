package fr.xephi.authme.command.executable.authme.debug;

import ch.jalu.injector.factory.SingletonStore;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.datasource.CacheDataSource;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.HasCleanup;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.permission.DebugSectionPermissions;
import fr.xephi.authme.permission.PermissionNode;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

import static fr.xephi.authme.command.executable.authme.debug.DebugSectionUtils.applyToLimboPlayersMap;

/**
 * Fetches various statistics, particularly regarding in-memory data that is stored.
 */
class DataStatistics implements DebugSection {

    @Inject
    private PlayerCache playerCache;

    @Inject
    private LimboService limboService;

    @Inject
    private DataSource dataSource;

    @Inject
    private SingletonStore<Object> singletonStore;

    @Override
    public String getName() {
        return "stats";
    }

    @Override
    public String getDescription() {
        return "Outputs general data statistics";
    }

    @Override
    public void execute(CommandSender sender, List<String> arguments) {
        sender.sendMessage(ChatColor.BLUE + "AuthMe statistics");
        sender.sendMessage("LimboPlayers in memory: " + applyToLimboPlayersMap(limboService, Map::size));
        sender.sendMessage("PlayerCache size: " + playerCache.getLogged() + " (= logged in players)");

        outputDatabaseStats(sender);
        outputInjectorStats(sender);
    }

    @Override
    public PermissionNode getRequiredPermission() {
        return DebugSectionPermissions.DATA_STATISTICS;
    }

    private void outputDatabaseStats(CommandSender sender) {
        sender.sendMessage("Total players in DB: " + dataSource.getAccountsRegistered());
        if (dataSource instanceof CacheDataSource) {
            CacheDataSource cacheDataSource = (CacheDataSource) this.dataSource;
            sender.sendMessage("Cached PlayerAuth objects: " + cacheDataSource.getCachedAuths().size());
        }
    }

    private void outputInjectorStats(CommandSender sender) {
        sender.sendMessage("Singleton Java classes: " + singletonStore.retrieveAllOfType().size());
        sender.sendMessage(String.format("(Reloadable: %d / SettingsDependent: %d / HasCleanup: %d)",
            singletonStore.retrieveAllOfType(Reloadable.class).size(),
            singletonStore.retrieveAllOfType(SettingsDependent.class).size(),
            singletonStore.retrieveAllOfType(HasCleanup.class).size()));
    }
}
