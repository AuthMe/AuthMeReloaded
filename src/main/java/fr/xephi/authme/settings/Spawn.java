package fr.xephi.authme.settings;

import java.io.File;
import org.bukkit.Bukkit;
import org.bukkit.Location;

/**
 *
 * @author Xephi59
 */
public class Spawn extends CustomConfiguration {

    private static Spawn spawn;

    public Spawn() {
        super(new File("." + File.separator + "plugins" + File.separator + "AuthMe" + File.separator + "spawn.yml"));
        spawn = this;
        load();
        save();
        saveDefault();
    }

    private void saveDefault() {
        if (!contains("spawn")) {
            set("spawn.world", "");
            set("spawn.x", "");
            set("spawn.y", "");
            set("spawn.z", "");
            set("spawn.yaw", "");
            set("spawn.pitch", "");
            save();
        }
        if (!contains("firstspawn")) {
            set("firstspawn.world", "");
            set("firstspawn.x", "");
            set("firstspawn.y", "");
            set("firstspawn.z", "");
            set("firstspawn.yaw", "");
            set("firstspawn.pitch", "");
            save();
        }
    }

    public static Spawn getInstance() {
        if (spawn == null) {
            spawn = new Spawn();
        }
        return spawn;
    }

    public boolean setSpawn(Location location) {
        try {
            set("spawn.world", location.getWorld().getName());
            set("spawn.x", location.getX());
            set("spawn.y", location.getY());
            set("spawn.z", location.getZ());
            set("spawn.yaw", location.getYaw());
            set("spawn.pitch", location.getPitch());
            save();
            return true;
        } catch (NullPointerException npe) {
            return false;
        }
    }

    public boolean setFirstSpawn(Location location) {
        try {
            set("firstspawn.world", location.getWorld().getName());
            set("firstspawn.x", location.getX());
            set("firstspawn.y", location.getY());
            set("firstspawn.z", location.getZ());
            set("firstspawn.yaw", location.getYaw());
            set("firstspawn.pitch", location.getPitch());
            save();
            return true;
        } catch (NullPointerException npe) {
            return false;
        }
    }

    @Deprecated
    public Location getLocation() {
        return getSpawn();
    }

    public Location getSpawn() {
        try {
            if (this.getString("spawn.world").isEmpty() || this.getString("spawn.world") == "")
                return null;
            Location location = new Location(Bukkit.getWorld(this.getString("spawn.world")), this.getDouble("spawn.x"), this.getDouble("spawn.y"), this.getDouble("spawn.z"), Float.parseFloat(this.getString("spawn.yaw")), Float.parseFloat(this.getString("spawn.pitch")));
            return location;
        } catch (NullPointerException npe) {
            return null;
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    public Location getFirstSpawn() {
        try {
            if (this.getString("firstspawn.world").isEmpty() || this.getString("firstspawn.world") == "")
                return null;
            Location location = new Location(Bukkit.getWorld(this.getString("firstspawn.world")), this.getDouble("firstspawn.x"), this.getDouble("firstspawn.y"), this.getDouble("firstspawn.z"), Float.parseFloat(this.getString("firstspawn.yaw")), Float.parseFloat(this.getString("firstspawn.pitch")));
            return location;
        } catch (NullPointerException npe) {
            return null;
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

}
