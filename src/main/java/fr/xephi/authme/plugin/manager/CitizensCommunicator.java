package fr.xephi.authme.plugin.manager;

import org.bukkit.entity.Entity;

import fr.xephi.authme.AuthMe;
import net.citizensnpcs.api.CitizensAPI;

public class CitizensCommunicator {

    public AuthMe instance;

    public CitizensCommunicator(AuthMe instance) {
        this.instance = instance;
    }

    public boolean isNPC(final Entity player) {
        if (!this.instance.isCitizensActive)
            return false;
        try {
            return CitizensAPI.getNPCRegistry().isNPC(player);
        } catch (NoClassDefFoundError ncdfe) {
            return false;
        } catch (Exception npe) {
            return false;
        }
    }
}
