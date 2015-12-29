package fr.xephi.authme.util;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.events.AuthMeTeleportEvent;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.Settings;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Utility class for various operations used in the codebase.
 */
public final class Utils {

    private static AuthMe plugin;
    private static Wrapper wrapper;

    private static boolean getOnlinePlayersIsCollection = false;
    private static Method getOnlinePlayers;

    static {
        wrapper = Wrapper.getInstance();
        plugin = wrapper.getAuthMe();
        initializeOnlinePlayersIsCollectionField();
    }

    private Utils() {
        // Utility class
    }

    /**
     * Set the group of a player, by its AuthMe group type.
     *
     * @param player The player.
     * @param group  The group type.
     *
     * @return True if succeeded, false otherwise. False is also returned if groups aren't supported
     * with the current permissions system.
     */
    public static boolean setGroup(Player player, GroupType group) {
        // Check whether the permissions check is enabled
        if (!Settings.isPermissionCheckEnabled) {
            return false;
        }

        // Get the permissions manager, and make sure it's valid
        PermissionsManager permsMan = plugin.getPermissionsManager();
        if (permsMan == null) {
            ConsoleLogger.showError("Failed to access permissions manager instance, shutting down.");
            return false;
        }

        // Make sure group support is available
        if (!permsMan.hasGroupSupport()) {
            ConsoleLogger.showError("The current permissions system doesn't have group support, unable to set group!");
            return false;
        }

        switch (group) {
            case UNREGISTERED:
                // Remove the other group type groups, set the current group
                permsMan.removeGroups(player, Arrays.asList(Settings.getRegisteredGroup, Settings.getUnloggedinGroup));
                return permsMan.addGroup(player, Settings.unRegisteredGroup);

            case REGISTERED:
                // Remove the other group type groups, set the current group
                permsMan.removeGroups(player, Arrays.asList(Settings.unRegisteredGroup, Settings.getUnloggedinGroup));
                return permsMan.addGroup(player, Settings.getRegisteredGroup);

            case NOTLOGGEDIN:
                // Remove the other group type groups, set the current group
                permsMan.removeGroups(player, Arrays.asList(Settings.unRegisteredGroup, Settings.getRegisteredGroup));
                return permsMan.addGroup(player, Settings.getUnloggedinGroup);

            case LOGGEDIN:
                // Get the limbo player data
                LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(player.getName().toLowerCase());
                if (limbo == null)
                    return false;

                // Get the players group
                String realGroup = limbo.getGroup();

                // Remove the other group types groups, set the real group
                permsMan.removeGroups(player, Arrays.asList(Settings.unRegisteredGroup, Settings.getRegisteredGroup, Settings.getUnloggedinGroup));
                return permsMan.addGroup(player, realGroup);

            default:
                return false;
        }
    }

    /**
     * TODO: This method requires better explanation.
     * <p>
     * Set the normal group of a player.
     *
     * @param player The player.
     * @param group  The normal group.
     *
     * @return True on success, false on failure.
     */
    public static boolean addNormal(Player player, String group) {
        if (!Settings.isPermissionCheckEnabled) {
            return false;
        }

        // Get the permissions manager, and make sure it's valid
        PermissionsManager permsMan = plugin.getPermissionsManager();
        if (permsMan == null) {
            ConsoleLogger.showError("Failed to access permissions manager instance, aborting.");
            return false;
        }

        // Remove old groups
        permsMan.removeGroups(player, Arrays.asList(Settings.unRegisteredGroup,
            Settings.getRegisteredGroup, Settings.getUnloggedinGroup));

        // Add the normal group, return the result
        return permsMan.addGroup(player, group);
    }

    // TODO: Move to a Manager
    public static boolean checkAuth(Player player) {
        if (player == null || Utils.isUnrestricted(player)) {
            return true;
        }

        if (PlayerCache.getInstance().isAuthenticated(player.getName())) {
            return true;
        }

        if (!Settings.isForcedRegistrationEnabled && !plugin.getDataSource().isAuthAvailable(player.getName())) {
            return true;
        }
        return false;
    }

    public static boolean isUnrestricted(Player player) {
        return Settings.isAllowRestrictedIp
            && !Settings.getUnrestrictedName.isEmpty()
            && (Settings.getUnrestrictedName.contains(player.getName().toLowerCase()));
    }

    public static void packCoords(double x, double y, double z, String w, final Player pl) {
        World theWorld;
        if (w.equals("unavailableworld")) {
            theWorld = pl.getWorld();
        } else {
            theWorld = Bukkit.getWorld(w);
        }
        if (theWorld == null) {
            theWorld = pl.getWorld();
        }
        final World world = theWorld;
        final Location loc = new Location(world, x, y, z);

        Bukkit.getScheduler().scheduleSyncDelayedTask(wrapper.getAuthMe(), new Runnable() {

            @Override
            public void run() {
                AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(pl, loc);
                wrapper.getServer().getPluginManager().callEvent(tpEvent);
                if (!tpEvent.isCancelled()) {
                    pl.teleport(tpEvent.getTo());
                }
            }
        });
    }

    /**
     * Safe way to retrieve the list of online players from the server. Depending on the
     * implementation of the server, either an array of {@link Player} instances is being returned,
     * or a Collection. Always use this wrapper to retrieve online players instead of {@link
     * Bukkit#getOnlinePlayers()} directly.
     *
     * @return collection of online players
     *
     * @see <a href="https://www.spigotmc.org/threads/solved-cant-use-new-getonlineplayers.33061/">SpigotMC
     * forum</a>
     * @see <a href="http://stackoverflow.com/questions/32130851/player-changed-from-array-to-collection">StackOverflow</a>
     */
    @SuppressWarnings("unchecked")
    public static Collection<? extends Player> getOnlinePlayers() {
        if (getOnlinePlayersIsCollection) {
            return Bukkit.getOnlinePlayers();
        }
        try {
            // The lookup of a method via Reflections is rather expensive, so we keep a reference to it
            if (getOnlinePlayers == null) {
                getOnlinePlayers = Bukkit.class.getDeclaredMethod("getOnlinePlayers");
            }
            Object obj = getOnlinePlayers.invoke(null);
            if (obj instanceof Collection<?>) {
                return (Collection<? extends Player>) obj;
            } else if (obj instanceof Player[]) {
                return Arrays.asList((Player[]) obj);
            } else {
                String type = (obj != null) ? obj.getClass().getName() : "null";
                ConsoleLogger.showError("Unknown list of online players of type " + type);
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            ConsoleLogger.showError("Could not retrieve list of online players: ["
                + e.getClass().getName() + "] " + e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Method run when the Utils class is loaded to verify whether or not the Bukkit implementation
     * returns the online players as a Collection.
     *
     * @see Utils#getOnlinePlayers()
     */
    private static void initializeOnlinePlayersIsCollectionField() {
        try {
            Method method = Bukkit.class.getDeclaredMethod("getOnlinePlayers");
            getOnlinePlayersIsCollection = method.getReturnType() == Collection.class;
        } catch (NoSuchMethodException e) {
            ConsoleLogger.showError("Error verifying if getOnlinePlayers is a collection! Method doesn't exist");
        }
    }

    public static Player getPlayer(String name) {
        name = name.toLowerCase();
        return wrapper.getServer().getPlayer(name);
    }

    public static boolean isNPC(Player player) {
        return player.hasMetadata("NPC") || plugin.combatTagPlus != null
            && plugin.combatTagPlus.getNpcPlayerHelper().isNpc(player);
    }

    public static void teleportToSpawn(Player player) {
        if (Settings.isTeleportToSpawnEnabled && !Settings.noTeleport) {
            Location spawn = plugin.getSpawnLocation(player);
            AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(player, spawn);
            wrapper.getServer().getPluginManager().callEvent(tpEvent);
            if (!tpEvent.isCancelled()) {
                player.teleport(tpEvent.getTo());
            }
        }
    }

    /**
     */
    public enum GroupType {
        UNREGISTERED,
        REGISTERED,
        NOTLOGGEDIN,
        LOGGEDIN
    }
}
