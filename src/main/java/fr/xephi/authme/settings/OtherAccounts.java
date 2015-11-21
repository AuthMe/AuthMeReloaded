package fr.xephi.authme.settings;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 *
 * @author Xephi59
 * @version $Revision: 1.0 $
 */
public class OtherAccounts extends CustomConfiguration {

    private static OtherAccounts others = null;

    public OtherAccounts() {
        super(new File("." + File.separator + "plugins" + File.separator + "AuthMe" + File.separator + "otheraccounts.yml"));
        others = this;
        load();
        save();
    }

    /**
     * Method clear.
     * @param uuid UUID
     */
    public void clear(UUID uuid) {
        set(uuid.toString(), new ArrayList<String>());
        save();
    }

    /**
     * Method getInstance.
     * @return OtherAccounts
     */
    public static OtherAccounts getInstance() {
        if (others == null) {
            others = new OtherAccounts();
        }
        return others;
    }

    /**
     * Method addPlayer.
     * @param uuid UUID
     */
    public void addPlayer(UUID uuid) {
        try {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null)
                return;
            if (!this.getStringList(uuid.toString()).contains(player.getName())) {
                this.getStringList(uuid.toString()).add(player.getName());
                save();
            }
        } catch (NoSuchMethodError | Exception e) {
            //ignore
        }
    }

    /**
     * Method removePlayer.
     * @param uuid UUID
     */
    public void removePlayer(UUID uuid) {
        try {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null)
                return;
            if (this.getStringList(uuid.toString()).contains(player.getName())) {
                this.getStringList(uuid.toString()).remove(player.getName());
                save();
            }
        } catch (NoSuchMethodError | Exception e) {
            //ignore
        }
    }

    /**
     * Method getAllPlayersByUUID.
     * @param uuid UUID
     * @return List<String>
     */
    public List<String> getAllPlayersByUUID(UUID uuid) {
        return this.getStringList(uuid.toString());
    }
}
