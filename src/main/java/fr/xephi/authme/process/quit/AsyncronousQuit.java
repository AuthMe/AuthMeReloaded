package fr.xephi.authme.process.quit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.Utils;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.RestoreInventoryEvent;
import fr.xephi.authme.listener.AuthMePlayerListener;
import fr.xephi.authme.settings.Settings;

public class AsyncronousQuit {

    protected AuthMe plugin;
    protected DataSource database;
    protected Player player;
    private String name;
    private ItemStack[] armor = null;
    private ItemStack[] inv = null;
    private boolean isOp = false;
    private boolean isFlying = false;
    private boolean needToChange = false;
    private boolean isKick = false;

    public AsyncronousQuit(Player p, AuthMe plugin, DataSource database,
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
        if (Utils.isNPC(player) || Utils.isUnrestricted(player)) {
            return;
        }

        String ip = plugin.getIP(player);

        if (PlayerCache.getInstance().isAuthenticated(name)) {
            if (Settings.isSaveQuitLocationEnabled && database.isAuthAvailable(name)) {
                Location loc = player.getLocation();
                PlayerAuth auth = new PlayerAuth(name, loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName(), player.getName());
                database.updateQuitLoc(auth);
            }
            PlayerAuth auth = new PlayerAuth(name, ip, System.currentTimeMillis(), player.getName());
            database.updateSession(auth);
        }

        if (LimboCache.getInstance().hasLimboPlayer(name)) {
            LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(name);
            if (Settings.protectInventoryBeforeLogInEnabled && player.hasPlayedBefore()) {
                inv = limbo.getInventory();
                armor = limbo.getArmour();
            }
            if (limbo.getGroup() != null && !limbo.getGroup().equals(""))
                Utils.addNormal(player, limbo.getGroup());
            needToChange = true;
            isOp = limbo.getOperator();
            isFlying = limbo.isFlying();
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
        AuthMePlayerListener.gameMode.remove(name);
        final Player p = player;
        RestoreInventoryEvent ev = new RestoreInventoryEvent(player, inv, armor, true);
        Bukkit.getPluginManager().callEvent(ev);
        if (ev.isCancelled()) {
            inv = null;
            armor = null;
        } else {
            inv = ev.getInventory();
            armor = ev.getArmor();
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new ProcessSyncronousPlayerQuit(plugin, p, inv, armor, isOp, isFlying, needToChange));
    }
}
