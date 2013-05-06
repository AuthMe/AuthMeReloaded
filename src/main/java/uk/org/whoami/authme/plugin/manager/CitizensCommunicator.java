package uk.org.whoami.authme.plugin.manager;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.CitizensManager;

import org.bukkit.entity.Entity;

import uk.org.whoami.authme.AuthMe;

public class CitizensCommunicator {

    public AuthMe instance;

    public CitizensCommunicator(AuthMe instance) {
    	this.instance = instance;
    }

    public boolean isNPC(final Entity player, AuthMe instance) {
    	try {
        	if (instance.CitizensVersion == 1) {
        		return CitizensManager.isNPC(player);
        	} else if (instance.CitizensVersion == 2) {
        		return CitizensAPI.getNPCRegistry().isNPC(player);
        	} else {
        		return false;
        	}
    	} catch (NoClassDefFoundError ncdfe) {
    		return false;
    	} catch (NullPointerException npe) {
    		return false;
    	}
    }
}
