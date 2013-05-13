package uk.org.whoami.authme.settings;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;

/**
*
* @author Xephi59
*/
public class Spawn extends CustomConfiguration {

	private static Spawn spawn;
	private static List<String> emptyList = new ArrayList<String>();

	public Spawn() {
		super(new File("./plugins/AuthMe/spawn.yml"));
		spawn = this;
		load();
		save();
		saveDefault();
	}

	private void saveDefault() {
		if (!contains("spawn")) {
			set("spawn", emptyList);
			set("spawn.world", "");
			set("spawn.x", "");
			set("spawn.y", "");
			set("spawn.z", "");
			set("spawn.yaw", "");
			set("spawn.pitch", "");
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

	public Location getLocation() {
		try {
			if (this.getString("spawn.world").isEmpty() || this.getString("spawn.world") == "") return null;
			Location location = new Location(Bukkit.getWorld(this.getString("spawn.world")), this.getDouble("spawn.x"), this.getDouble("spawn.y"), this.getDouble("spawn.z"), Float.parseFloat(this.getString("spawn.yaw")), Float.parseFloat(this.getString("spawn.pitch")));
			return location;
		} catch (NullPointerException npe) {
			return null;
		} catch (NumberFormatException nfe) {
			return null;
		}
	}

}
