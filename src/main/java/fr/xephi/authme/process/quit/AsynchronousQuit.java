package fr.xephi.authme.process.quit;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 */
public class AsynchronousQuit {

    protected final AuthMe plugin;
    protected final DataSource database;
    protected final Player player;
    private final String name;
    private boolean isOp = false;
    private boolean needToChange = false;
    private boolean isKick = false;

    /**
     * Constructor for AsynchronousQuit.
     *
     * @param p        Player
     * @param plugin   AuthMe
     * @param database DataSource
     * @param isKick   boolean
     */
    public AsynchronousQuit(Player p, AuthMe plugin, DataSource database,
                            boolean isKick) {
        this.player = p;
        this.plugin = plugin;
        this.database = database;
        this.name = p.getName().toLowerCase();
        this.isKick = isKick;
    }

    public void process() {
        if (player == null)
            return;
        if (Utils.isUnrestricted(player)) {
            return;
        }

        String ip = plugin.getIP(player);

        if (PlayerCache.getInstance().isAuthenticated(name)) {
            if (Settings.isSaveQuitLocationEnabled) {
                Location loc = player.getLocation();
                PlayerAuth auth = new PlayerAuth(name, loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName(), player.getName());
                database.updateQuitLoc(auth);
            }
            PlayerAuth auth = new PlayerAuth(name, ip, System.currentTimeMillis(), player.getName());
            database.updateSession(auth);
        }

        if (LimboCache.getInstance().hasLimboPlayer(name)) {
            LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(name);
            if (limbo.getGroup() != null && !limbo.getGroup().isEmpty())
                Utils.addNormal(player, limbo.getGroup());
            needToChange = true;
            isOp = limbo.getOperator();
            if (limbo.getTimeoutTaskId() != null)
                limbo.getTimeoutTaskId().cancel();
            if (limbo.getMessageTaskId() != null)
                limbo.getMessageTaskId().cancel();
            LimboCache.getInstance().deleteLimboPlayer(name);
        }
        if (Settings.isSessionsEnabled && !isKick) {
            if (Settings.getSessionTimeout != 0) {
                BukkitTask task = plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, new Runnable() {

                    @Override
                    public void run() {
                        PlayerCache.getInstance().removePlayer(name);
                        if (database.isLogged(name))
                            database.setUnlogged(name);
                        plugin.sessions.remove(name);
                    }

                }, Settings.getSessionTimeout * 20 * 60);
                plugin.sessions.put(name, task);
            }
        } else {
            PlayerCache.getInstance().removePlayer(name);
            database.setUnlogged(name);
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new ProcessSyncronousPlayerQuit(plugin, player, isOp, needToChange));
    }
}
