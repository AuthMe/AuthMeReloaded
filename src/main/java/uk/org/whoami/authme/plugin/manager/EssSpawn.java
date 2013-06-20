package uk.org.whoami.authme.plugin.manager;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import uk.org.whoami.authme.settings.CustomConfiguration;

/**
*
* @author Xephi59
*/
public class EssSpawn extends CustomConfiguration {

	private static EssSpawn spawn;

	public EssSpawn() {
		super(new File("./plugins/Essentials/spawn.yml"));
		spawn = this;
		load();
	}

	public static EssSpawn getInstance() {
        if (spawn == null) {
            spawn = new EssSpawn();
        }        
        return spawn;
    }

	public Location getLocation() {
		try {
			if (this.getString("spawns.default.world").isEmpty() || this.getString("spawns.default.world") == "") return null;
			Location location = new Location(Bukkit.getWorld(this.getString("spawns.default.world")), this.getDouble("spawns.default.x"), this.getDouble("spawns.default.y"), this.getDouble("spawns.default.z"), Float.parseFloat(this.getString("spawns.default.yaw")), Float.parseFloat(this.getString("spawns.default.pitch")));
			return location;
		} catch (NullPointerException npe) {
			return null;
		} catch (NumberFormatException nfe) {
			return null;
		}
	}

}
