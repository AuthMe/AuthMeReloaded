package fr.xephi.authme;

import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.events.AuthMeTeleportEvent;
import fr.xephi.authme.settings.Settings;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class Utils {

    private static boolean getOnlinePlayersIsCollection;
    private static Method getOnlinePlayers;
    public static AuthMe plugin;

    static {
        plugin = AuthMe.getInstance();
        try {
            Method m = Bukkit.class.getDeclaredMethod("getOnlinePlayers");
            getOnlinePlayersIsCollection = m.getReturnType() == Collection.class;
        } catch (Exception ignored) {
        }
    }

    public static void setGroup(Player player, GroupType group) {
        if (!Settings.isPermissionCheckEnabled)
            return;
        if (plugin.permission == null)
            return;
        String currentGroup;
        try {
            currentGroup = plugin.permission.getPrimaryGroup(player);
        } catch (UnsupportedOperationException e) {
            ConsoleLogger.showError("Your permission plugin (" + plugin.permission.getName() + ") doesn't support the Group system... unhook!");
            plugin.permission = null;
            return;
        }
        switch (group) {
            case UNREGISTERED: {
                plugin.permission.playerRemoveGroup(player, currentGroup);
                plugin.permission.playerAddGroup(player, Settings.unRegisteredGroup);
                break;
            }
            case REGISTERED: {
                plugin.permission.playerRemoveGroup(player, currentGroup);
                plugin.permission.playerAddGroup(player, Settings.getRegisteredGroup);
                break;
            }
            case NOTLOGGEDIN: {
                if (!useGroupSystem())
                    break;
                plugin.permission.playerRemoveGroup(player, currentGroup);
                plugin.permission.playerAddGroup(player, Settings.getUnloggedinGroup);
                break;
            }
            case LOGGEDIN: {
                if (!useGroupSystem())
                    break;
                LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(player.getName().toLowerCase());
                if (limbo == null)
                    break;
                String realGroup = limbo.getGroup();
                plugin.permission.playerRemoveGroup(player, currentGroup);
                plugin.permission.playerAddGroup(player, realGroup);
                break;
            }
        }
    }

    public static boolean addNormal(Player player, String group) {
        if (!useGroupSystem()) {
            return false;
        }
        if (plugin.permission == null)
            return false;
        try {
            if (plugin.permission.playerRemoveGroup(player, Settings.getUnloggedinGroup) && plugin.permission.playerAddGroup(player, group)) {
                return true;
            }
        } catch (UnsupportedOperationException e) {
            ConsoleLogger.showError("Your permission system (" + plugin.permission.getName() + ") do not support Group system with that config... unhook!");
            plugin.permission = null;
            return false;
        }
        return false;
    }

    // TODO: remove if not needed
    @SuppressWarnings("unused")
    public static void hasPermOnJoin(Player player) {
        if (plugin.permission == null)
            return;
        for (String permission : Settings.getJoinPermissions) {
            if (plugin.permission.playerHas(player, permission)) {
                plugin.permission.playerAddTransient(player, permission);
            }
        }
    }

    public static boolean isUnrestricted(Player player) {
        return Settings.isAllowRestrictedIp && !(Settings.getUnrestrictedName == null || Settings.getUnrestrictedName.isEmpty()) && (Settings.getUnrestrictedName.contains(player.getName()));
    }

    private static boolean useGroupSystem() {
        return Settings.isPermissionCheckEnabled && !Settings.getUnloggedinGroup.isEmpty();
    }

    public static void packCoords(double x, double y, double z, String w,
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
     * Used for force player GameMode
     */
    public static void forceGM(Player player) {
        if (!plugin.authmePermissible(player, "authme.bypassforcesurvival"))
            player.setGameMode(GameMode.SURVIVAL);
    }

    public enum GroupType {
        UNREGISTERED,
        REGISTERED,
        NOTLOGGEDIN,
        LOGGEDIN
    }

    public static void purgeDirectory(File file) {
        if (!file.isDirectory()) {
            return;
        }
        File[] files = file.listFiles();
        if (files == null) {
            return;
        }
        for (File target : files) {
            if (target.isDirectory()) {
                purgeDirectory(target);
                target.delete();
            } else {
                target.delete();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static Collection<? extends Player> getOnlinePlayers() {
        if (getOnlinePlayersIsCollection) {
            return Bukkit.getOnlinePlayers();
        }
        try {
            if (getOnlinePlayers == null) {
                getOnlinePlayers = Bukkit.class.getMethod("getOnlinePlayers");
            }
            Object obj = getOnlinePlayers.invoke(null);
            if (obj instanceof Collection) {
                return (Collection<? extends Player>) obj;
            }
            return Arrays.asList((Player[]) obj);
        } catch (Exception ignored) {
        }
        return Collections.emptyList();
    }

    public static boolean isNPC(final Entity player) {
        try {
            if (player.hasMetadata("NPC")) {
                return true;
            } else if (plugin.combatTagPlus != null
                    && player instanceof Player
                    && plugin.combatTagPlus.getNpcPlayerHelper().isNpc((Player) player)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
