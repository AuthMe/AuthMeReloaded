package fr.xephi.authme;

import fr.xephi.authme.hooks.PluginHooks;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.PurgeSettings;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.Utils;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;

import javax.inject.Inject;
import java.io.File;

import static fr.xephi.authme.util.StringUtils.makePath;
import java.util.Set;

/**
 */
public class DataManager {

    @Inject
    private Server server;
    @Inject
    private PluginHooks pluginHooks;
    @Inject
    private BukkitService bukkitService;
    @Inject
    private NewSetting settings;
    @Inject
    private PermissionsManager permissionsManager;

    DataManager() { }

    public void purgeAntiXray(Set<String> cleared) {
        int i = 0;
        File dataFolder = new File("." + File.separator + "plugins" + File.separator + "AntiXRayData"
            + File.separator + "PlayerData");
        if (!dataFolder.exists() || !dataFolder.isDirectory()) {
            return;
        }

        for (String file : dataFolder.list()) {
            if (cleared.contains(file.toLowerCase())) {
                File playerFile = new File(dataFolder, file);
                if (playerFile.exists() && playerFile.delete()) {
                    i++;
                }
            }
        }

        ConsoleLogger.info("AutoPurge: Removed " + i + " AntiXRayData Files");
    }

    public synchronized void purgeLimitedCreative(Set<String> cleared) {
        int i = 0;
        File dataFolder = new File("." + File.separator + "plugins" + File.separator + "LimitedCreative"
            + File.separator + "inventories");
        if (!dataFolder.exists() || !dataFolder.isDirectory()) {
            return;
        }
        for (String file : dataFolder.list()) {
            String name = file;
            int idx;
            idx = file.lastIndexOf("_creative.yml");
            if (idx != -1) {
                name = name.substring(0, idx);
            } else {
                idx = file.lastIndexOf("_adventure.yml");
                if (idx != -1) {
                    name = name.substring(0, idx);
                } else {
                    idx = file.lastIndexOf(".yml");
                    if (idx != -1) {
                        name = name.substring(0, idx);
                    }
                }
            }
            if (name.equals(file)) {
                continue;
            }
            if (cleared.contains(name.toLowerCase())) {
                File dataFile = new File(dataFolder, file);
                if (dataFile.exists() && dataFile.delete()) {
                    i++;
                }
            }
        }
        ConsoleLogger.info("AutoPurge: Removed " + i + " LimitedCreative Survival, Creative and Adventure files");
    }

    public synchronized void purgeDat(Set<OfflinePlayer> cleared) {
        int i = 0;
        File dataFolder = new File(server.getWorldContainer()
                , makePath(settings.getProperty(PurgeSettings.DEFAULT_WORLD), "players"));

        for (OfflinePlayer offlinePlayer : cleared) {
            File playerFile = new File(dataFolder, Utils.getUUIDorName(offlinePlayer) + ".dat");
            if (playerFile.delete()) {
                i++;
            }
        }

        ConsoleLogger.info("AutoPurge: Removed " + i + " .dat Files");
    }

    /**
     * Method purgeEssentials.
     *
     * @param cleared List of String
     */
    public void purgeEssentials(Set<OfflinePlayer> cleared) {
        int i = 0;
        File essentialsDataFolder = pluginHooks.getEssentialsDataFolder();
        if (essentialsDataFolder == null) {
            ConsoleLogger.info("Cannot purge Essentials: plugin is not loaded");
            return;
        }

        final File userDataFolder = new File(essentialsDataFolder, "userdata");
        if (!userDataFolder.exists() || !userDataFolder.isDirectory()) {
            return;
        }

        for (OfflinePlayer offlinePlayer : cleared) {
            File playerFile = new File(userDataFolder, Utils.getUUIDorName(offlinePlayer) + ".yml");
            if (playerFile.exists() && playerFile.delete()) {
                i++;
            }
        }

        ConsoleLogger.info("AutoPurge: Removed " + i + " EssentialsFiles");
    }

    // TODO: What is this method for? Is it correct?
    // TODO: Make it work with OfflinePlayers group data.
    public synchronized void purgePermissions(Set<OfflinePlayer> cleared) {
        for (OfflinePlayer offlinePlayer : cleared) {
            String name = offlinePlayer.getName();
            permissionsManager.removeAllGroups(bukkitService.getPlayerExact(name));
        }

        ConsoleLogger.info("AutoPurge: Removed permissions from " + cleared.size() + " player(s).");
    }
}
