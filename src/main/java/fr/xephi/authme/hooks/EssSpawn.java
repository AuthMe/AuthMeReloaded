package fr.xephi.authme.hooks;

import fr.xephi.authme.settings.CustomConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.File;

/**
 */
public class EssSpawn extends CustomConfiguration {

    private static EssSpawn spawn;

    public EssSpawn() {
        super(new File("." + File.separator + "plugins" + File.separator + "Essentials" + File.separator + "spawn.yml"), true);
        spawn = this;
        load();
    }

    /**
     * Method getInstance.
     *
     * @return EssSpawn
     */
    public static EssSpawn getInstance() {
        if (spawn == null) {
            spawn = new EssSpawn();
        }
        return spawn;
    }

    /**
     * Method getLocation.
     *
     * @return Location
     */
    public Location getLocation() {
        try {
            if (!this.contains("spawns.default.world"))
                return null;
            if (this.getString("spawns.default.world").isEmpty() || this.getString("spawns.default.world").equals(""))
                return null;
            return new Location(Bukkit.getWorld(this.getString("spawns.default.world")), this.getDouble("spawns.default.x"), this.getDouble("spawns.default.y"), this.getDouble("spawns.default.z"), Float.parseFloat(this.getString("spawns.default.yaw")), Float.parseFloat(this.getString("spawns.default.pitch")));
        } catch (NullPointerException | NumberFormatException npe) {
            return null;
        }
    }

}
