package fr.xephi.authme;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.events.AuthMeTeleportEvent;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.settings.Settings;

public class Utils {

    private String currentGroup;
    private static Utils singleton;
    int id;
    public AuthMe plugin;
    private static List<String> tokens = new ArrayList<String>();

    public Utils(AuthMe plugin) {
        this.plugin = plugin;
    }

    public void setGroup(Player player, groupType group) {
        setGroup(player.getName(), group);
    }

    @SuppressWarnings("deprecation")
    public void setGroup(String player, groupType group) {
        if (!Settings.isPermissionCheckEnabled)
            return;
        if (plugin.permission == null)
            return;
        String name = player;
        try {
            World world = null;
            currentGroup = plugin.permission.getPrimaryGroup(world, name);
        } catch (UnsupportedOperationException e) {
            ConsoleLogger.showError("Your permission plugin (" + plugin.permission.getName() + ") doesn't support the Group system... unhook!");
            plugin.permission = null;
            return;
        }
        World world = null;
        switch (group) {
            case UNREGISTERED: {
                plugin.permission.playerRemoveGroup(world, name, currentGroup);
                plugin.permission.playerAddGroup(world, name, Settings.unRegisteredGroup);
                break;
            }
            case REGISTERED: {
                plugin.permission.playerRemoveGroup(world, name, currentGroup);
                plugin.permission.playerAddGroup(world, name, Settings.getRegisteredGroup);
                break;
            }
            case NOTLOGGEDIN: {
                if (!useGroupSystem())
                    break;
                plugin.permission.playerRemoveGroup(world, name, currentGroup);
                plugin.permission.playerAddGroup(world, name, Settings.getUnloggedinGroup);
                break;
            }
            case LOGGEDIN: {
                if (!useGroupSystem())
                    break;
                LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(name.toLowerCase());
                if (limbo == null)
                    break;
                String realGroup = limbo.getGroup();
                plugin.permission.playerRemoveGroup(world, name, currentGroup);
                plugin.permission.playerAddGroup(world, name, realGroup);
                break;
            }
        }
        return;
    }

    @SuppressWarnings("deprecation")
    public boolean addNormal(Player player, String group) {
        if (!useGroupSystem()) {
            return false;
        }
        if (plugin.permission == null)
            return false;
        World world = null;
        try {
            if (plugin.permission.playerRemoveGroup(world, player.getName().toString(), Settings.getUnloggedinGroup) && plugin.permission.playerAddGroup(world, player.getName().toString(), group)) {
                return true;
            }
        } catch (UnsupportedOperationException e) {
            ConsoleLogger.showError("Your permission system (" + plugin.permission.getName() + ") do not support Group system with that config... unhook!");
            plugin.permission = null;
            return false;
        }
        return false;
    }

    public void hasPermOnJoin(Player player) {
        if (plugin.permission == null)
            return;
        Iterator<String> iter = Settings.getJoinPermissions.iterator();
        while (iter.hasNext()) {
            String permission = iter.next();
            if (plugin.permission.playerHas(player, permission)) {
                plugin.permission.playerAddTransient(player, permission);
            }
        }
    }

    public boolean isUnrestricted(Player player) {
        if (!Settings.isAllowRestrictedIp)
            return false;
        if (Settings.getUnrestrictedName.isEmpty() || Settings.getUnrestrictedName == null)
            return false;
        if (Settings.getUnrestrictedName.contains(player.getName()))
            return true;
        return false;
    }

    public static Utils getInstance() {
        singleton = new Utils(AuthMe.getInstance());
        return singleton;
    }

    private boolean useGroupSystem() {
        if (Settings.isPermissionCheckEnabled && !Settings.getUnloggedinGroup.isEmpty())
            return true;
        return false;
    }

    public void packCoords(double x, double y, double z, String w,
            final Player pl) {
        World theWorld;
        if (w.equals("unavailableworld")) {
            theWorld = pl.getWorld();
        } else {
            theWorld = Bukkit.getWorld(w);
        }
        if (theWorld == null)
            theWorld = pl.getWorld();
        final World world = theWorld;
        final Location locat = new Location(world, x, y, z);

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

            @Override
            public void run() {
                AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(pl, locat);
                plugin.getServer().getPluginManager().callEvent(tpEvent);
                if (!tpEvent.isCancelled()) {
                    if (!tpEvent.getTo().getChunk().isLoaded())
                        tpEvent.getTo().getChunk().load();
                    pl.teleport(tpEvent.getTo());
                }
            }
        });
    }

    /*
     * Random Token for passpartu
     */
    public boolean obtainToken() {
        try {
            final String token = new RandomString(10).nextString();
            tokens.add(token);
            ConsoleLogger.info("[AuthMe] Security passpartu token: " + token);
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, new Runnable() {

                @Override
                public void run() {
                    tokens.remove(token);
                }

            }, 600);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /*
     * Read Token
     */
    public boolean readToken(String inputToken) {
        boolean ret = false;
        if (tokens.contains(inputToken))
            ret = true;
        tokens.remove(inputToken);
        return (ret);
    }

    /*
     * Used for force player GameMode
     */
    public static void forceGM(Player player) {
        if (!AuthMe.getInstance().authmePermissible(player, "authme.bypassforcesurvival"))
            player.setGameMode(GameMode.SURVIVAL);
    }

    public enum groupType {
        UNREGISTERED,
        REGISTERED,
        NOTLOGGEDIN,
        LOGGEDIN
    }

}
