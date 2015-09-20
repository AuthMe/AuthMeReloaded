package fr.xephi.authme;

import com.maxmind.geoip.LookupService;
import fr.xephi.authme.cache.auth.PlayerCache;
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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.zip.GZIPInputStream;

public class Utils {

    public static AuthMe plugin;

    private static boolean getOnlinePlayersIsCollection;
    private static Method getOnlinePlayers;
    private static LookupService lookupService;

    static {
        plugin = AuthMe.getInstance();
        checkGeoIP();
        try {
            Method m = Bukkit.class.getDeclaredMethod("getOnlinePlayers");
            getOnlinePlayersIsCollection = m.getReturnType() == Collection.class;
        } catch (Exception ignored) {
        }
    }

    // Check and Download GeoIP data if not exist
    public static boolean checkGeoIP() {
        if (lookupService != null) {
            return true;
        }
        ConsoleLogger.info("[LICENSE] This product uses data from the GeoLite API created by MaxMind, available at http://www.maxmind.com");
        File file = new File(Settings.PLUGIN_FOLDER, "GeoIP.dat");
        try {
            if (file.exists()) {
                if (lookupService == null) {
                    lookupService = new LookupService(file);
                    return true;
                }
            }
            String url = "http://geolite.maxmind.com/download/geoip/database/GeoLiteCountry/GeoIP.dat.gz";
            URL downloadUrl = new URL(url);
            URLConnection conn = downloadUrl.openConnection();
            conn.setConnectTimeout(10000);
            conn.connect();
            InputStream input = conn.getInputStream();
            if (conn.getURL().toString().endsWith(".gz")) {
                input = new GZIPInputStream(input);
            }
            OutputStream output = new FileOutputStream(file);
            byte[] buffer = new byte[2048];
            int length = input.read(buffer);
            while (length >= 0) {
                output.write(buffer, 0, length);
                length = input.read(buffer);
            }
            output.close();
            input.close();
        } catch (Exception e) {
            ConsoleLogger.writeStackTrace(e);
            return false;
        }
        return checkGeoIP();
    }

    public static String getCountryCode(String ip) {
        if (checkGeoIP()) {
            return lookupService.getCountry(ip).getCode();
        }
        return "--";
    }

    public static String getCountryName(String ip) {
        if (checkGeoIP()) {
            return lookupService.getCountry(ip).getName();
        }
        return "N/A";
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

    // TODO: Move to a Manager
    public static boolean checkAuth(Player player) {
        if (player == null || Utils.isUnrestricted(player)) {
            return true;
        }
        String name = player.getName().toLowerCase();
        if (PlayerCache.getInstance().isAuthenticated(name)) {
            return true;
        }
        if (!plugin.database.isAuthAvailable(name)) {
            if (!Settings.isForcedRegistrationEnabled) {
                return true;
            }
        }
        return false;
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
