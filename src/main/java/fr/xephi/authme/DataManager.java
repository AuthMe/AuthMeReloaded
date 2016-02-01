package fr.xephi.authme;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.Utils;

/**
 */
public class DataManager {

    public final AuthMe plugin;

    /**
     * Constructor for DataManager.
     *
     * @param plugin AuthMe
     */
    public DataManager(AuthMe plugin) {
        this.plugin = plugin;
    }

    /**
     * Method getOfflinePlayer.
     *
     * @param name String
     *
     * @return OfflinePlayer
     */
    public synchronized OfflinePlayer getOfflinePlayer(final String name) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<OfflinePlayer> result = executor.submit(new Callable<OfflinePlayer>() {

            public synchronized OfflinePlayer call() throws Exception {
                OfflinePlayer result = null;
                try {
                    for (OfflinePlayer op : Bukkit.getOfflinePlayers())
                        if (op.getName().equalsIgnoreCase(name)) {
                            result = op;
                            break;
                        }
                } catch (Exception ignored) {
                }
                return result;
            }
        });
        try {
            return result.get();
        } catch (Exception e) {
            return (null);
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Method purgeAntiXray.
     *
     * @param cleared List of String
     */
    public synchronized void purgeAntiXray(List<String> cleared) {
        int i = 0;
        for (String name : cleared) {
            try {
                org.bukkit.OfflinePlayer player = getOfflinePlayer(name);
                if (player == null)
                    continue;
                String playerName = player.getName();
                File playerFile = new File("." + File.separator + "plugins" + File.separator + "AntiXRayData" + File.separator + "PlayerData" + File.separator + playerName);
                if (playerFile.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    playerFile.delete();
                    i++;
                }
            } catch (Exception ignored) {
            }
        }
        ConsoleLogger.info("AutoPurgeDatabase : Remove " + i + " AntiXRayData Files");
    }

    /**
     * Method purgeLimitedCreative.
     *
     * @param cleared List of String
     */
    public synchronized void purgeLimitedCreative(List<String> cleared) {
        int i = 0;
        for (String name : cleared) {
            try {
                org.bukkit.OfflinePlayer player = getOfflinePlayer(name);
                if (player == null)
                    continue;
                String playerName = player.getName();
                File playerFile = new File("." + File.separator + "plugins" + File.separator + "LimitedCreative" + File.separator + "inventories" + File.separator + playerName + ".yml");
                if (playerFile.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    playerFile.delete();
                    i++;
                }
                playerFile = new File("." + File.separator + "plugins" + File.separator + "LimitedCreative" + File.separator + "inventories" + File.separator + playerName + "_creative.yml");
                if (playerFile.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    playerFile.delete();
                    i++;
                }
                playerFile = new File("." + File.separator + "plugins" + File.separator + "LimitedCreative" + File.separator + "inventories" + File.separator + playerName + "_adventure.yml");
                if (playerFile.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    playerFile.delete();
                    i++;
                }
            } catch (Exception ignored) {
            }
        }
        ConsoleLogger.info("AutoPurgeDatabase : Remove " + i + " LimitedCreative Survival, Creative and Adventure files");
    }

    /**
     * Method purgeDat.
     *
     * @param cleared List of String
     */
    public synchronized void purgeDat(List<String> cleared) {
        int i = 0;
        for (String name : cleared) {
            try {
                org.bukkit.OfflinePlayer player = getOfflinePlayer(name);
                if (player == null) {
                    continue;
                }

                try {
                    File playerFile = new File(plugin.getServer().getWorldContainer() + File.separator + Settings.defaultWorld + File.separator + "players" + File.separator + player.getUniqueId() + ".dat");
                    //noinspection ResultOfMethodCallIgnored
                    playerFile.delete();
                    i++;
                } catch (Exception ignore) {
                    File playerFile = new File(plugin.getServer().getWorldContainer() + File.separator + Settings.defaultWorld + File.separator + "players" + File.separator + player.getName() + ".dat");
                    if (playerFile.exists()) {
                        //noinspection ResultOfMethodCallIgnored
                        playerFile.delete();
                        i++;
                    }
                }
            } catch (Exception ignore) {
            }
        }
        ConsoleLogger.info("AutoPurgeDatabase : Remove " + i + " .dat Files");
    }

    /**
     * Method purgeEssentials.
     *
     * @param cleared List of String
     */
    @SuppressWarnings("deprecation")
    public void purgeEssentials(List<String> cleared) {
        int i = 0;
        for (String name : cleared) {
            try {
                File playerFile = new File(plugin.ess.getDataFolder() + File.separator + "userdata" + File.separator + plugin.getServer().getOfflinePlayer(name).getUniqueId() + ".yml");
                //noinspection ResultOfMethodCallIgnored
                playerFile.delete();
                i++;
            } catch (Exception e) {
                File playerFile = new File(plugin.ess.getDataFolder() + File.separator + "userdata" + File.separator + name + ".yml");
                if (playerFile.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    playerFile.delete();
                    i++;
                }
            }
        }
        ConsoleLogger.info("AutoPurgeDatabase : Remove " + i + " EssentialsFiles");
    }

    // TODO: What is this method for? Is it correct?

    /**
     * @param cleared Cleared players.
     */
    public synchronized void purgePermissions(List<String> cleared) {
        // Get the permissions manager, and make sure it's valid
        PermissionsManager permsMan = this.plugin.getPermissionsManager();
        if (permsMan == null)
            ConsoleLogger.showError("Unable to access permissions manager instance!");
        assert permsMan != null;

        int i = 0;
        for (String name : cleared) {
            try {
                permsMan.removeAllGroups(this.getOnlinePlayerLower(name.toLowerCase()));
                i++;
            } catch (Exception ignored) {
            }
        }
        ConsoleLogger.info("AutoPurgeDatabase : Removed " + i + "permissions");

        /*int i = 0;
        for (String name : cleared) {
            try {
                OfflinePlayer p = this.getOfflinePlayer(name);
                for (String group : permission.getPlayerGroups((Player) p)) {
                    permission.playerRemoveGroup(null, p, group);
                }
                i++;
            } catch (Exception e) {
            }
        }
        ConsoleLogger.info("AutoPurgeDatabase : Remove " + i + " Permissions");*/
    }

    /**
     * Method isOnline.
     *
     * @param player Player
     * @param name   String
     *
     * @return boolean
     */
    public boolean isOnline(Player player, final String name) {
        if (player.isOnline())
            return true;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> result = executor.submit(new Callable<Boolean>() {

            @Override
            public synchronized Boolean call() throws Exception {
                for (OfflinePlayer op : Utils.getOnlinePlayers())
                    if (op.getName().equalsIgnoreCase(name)) {
                        return true;
                    }
                return false;
            }
        });
        try {
            return result.get();
        } catch (Exception e) {
            return false;
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Method getOnlinePlayerLower.
     *
     * @param name String
     *
     * @return Player
     */
    public Player getOnlinePlayerLower(String name) {
        name = name.toLowerCase();
        for (Player player : Utils.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(name))
                return player;
        }
        return null;
    }
}
