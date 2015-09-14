package fr.xephi.authme;

import fr.xephi.authme.settings.Settings;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DataManager {

    public AuthMe plugin;

    public DataManager(AuthMe plugin) {
        this.plugin = plugin;
    }

    public void run() {
    }

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
                } catch (Exception e) {
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
                    playerFile.delete();
                    i++;
                }
            } catch (Exception e) {
            }
        }
        ConsoleLogger.info("AutoPurgeDatabase : Remove " + i + " AntiXRayData Files");
    }

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
                    playerFile.delete();
                    i++;
                }
                playerFile = new File("." + File.separator + "plugins" + File.separator + "LimitedCreative" + File.separator + "inventories" + File.separator + playerName + "_creative.yml");
                if (playerFile.exists()) {
                    playerFile.delete();
                    i++;
                }
                playerFile = new File("." + File.separator + "plugins" + File.separator + "LimitedCreative" + File.separator + "inventories" + File.separator + playerName + "_adventure.yml");
                if (playerFile.exists()) {
                    playerFile.delete();
                    i++;
                }
            } catch (Exception e) {
            }
        }
        ConsoleLogger.info("AutoPurgeDatabase : Remove " + i + " LimitedCreative Survival, Creative and Adventure files");
    }

    public synchronized void purgeDat(List<String> cleared) {
        int i = 0;
        for (String name : cleared) {
            try {
                org.bukkit.OfflinePlayer player = getOfflinePlayer(name);
                if (player == null)
                    continue;
                String playerName = player.getName();
                File playerFile = new File(plugin.getServer().getWorldContainer() + File.separator + Settings.defaultWorld + File.separator + "players" + File.separator + playerName + ".dat");
                if (playerFile.exists()) {
                    playerFile.delete();
                    i++;
                } else {
                    playerFile = new File(plugin.getServer().getWorldContainer() + File.separator + Settings.defaultWorld + File.separator + "players" + File.separator + player.getUniqueId() + ".dat");
                    if (playerFile.exists()) {
                        playerFile.delete();
                        i++;
                    }
                }
            } catch (Exception e) {
            }
        }
        ConsoleLogger.info("AutoPurgeDatabase : Remove " + i + " .dat Files");
    }

    @SuppressWarnings("deprecation")
    public void purgeEssentials(List<String> cleared) {
        int i = 0;
        for (String name : cleared) {
            try {
                File playerFile = new File(plugin.ess.getDataFolder() + File.separator + "userdata" + File.separator + name + ".yml");
                if (playerFile.exists()) {
                    playerFile.delete();
                    i++;
                } else {
                    try {
                        playerFile = new File(plugin.ess.getDataFolder() + File.separator + "userdata" + File.separator + Bukkit.getOfflinePlayer(name).getUniqueId() + ".yml");
                        if (playerFile.exists()) {
                            playerFile.delete();
                            i++;
                        }
                    } catch (Exception e) { // Don't do nothing if the method getUniqueId() isn't avariable ( MC version < 1.7.5 )
                    }
                }
            } catch (Exception e) {
            }
        }
        ConsoleLogger.info("AutoPurgeDatabase : Remove " + i + " EssentialsFiles");
    }

    public synchronized void purgePermissions(List<String> cleared,
                                              Permission permission) {
        int i = 0;
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
        ConsoleLogger.info("AutoPurgeDatabase : Remove " + i + " Permissions");
    }

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
}
