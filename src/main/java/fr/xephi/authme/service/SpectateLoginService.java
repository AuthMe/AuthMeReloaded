package fr.xephi.authme.service;

import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * Sets the player gamemode to Spectator, puts the player in an invisible armorstand and fixes the direction of the view
 */
public class SpectateLoginService {

    private Map<Player, ArmorStand> armorStands = new HashMap<>();
    private Map<Player, GameMode> gameModeMap = new HashMap<>();

    @Inject
    private CommonService service;

    /**
     * Creates a stand for the player
     *
     * @param player the player
     */
    public void createStand(Player player) {
        if (player.isDead()) {
            return;
        }
        Location location = player.getLocation();
        ArmorStand stand = spawnStand(location);

        armorStands.put(player, stand);
        gameModeMap.put(player, player.getGameMode());

        player.setGameMode(GameMode.SPECTATOR);
        player.setSpectatorTarget(stand);
    }

    /**
     * Updates spectator target for the player
     *
     * @param player the player
     */
    public void updateTarget(Player player) {
        ArmorStand stand = armorStands.get(player);
        if (stand != null) {
            player.setSpectatorTarget(stand);
        }
    }

    /**
     * Removes the player's stand and deletes effects
     *
     * @param player the player
     */
    public void removeStand(Player player) {
        ArmorStand stand = armorStands.get(player);
        if (stand != null) {

            stand.remove();
            player.setSpectatorTarget(null);
            player.setGameMode(gameModeMap.get(player));

            gameModeMap.remove(player);
            armorStands.remove(player);
        }
    }

    /**
     * Removes all armorstands and restores player gamemode
     */
    public void removeArmorstands() {
        for (Player player : armorStands.keySet()) {
            removeStand(player);
        }

        gameModeMap.clear();
        armorStands.clear();
    }

    public boolean hasStand(Player player) {
        return armorStands.containsKey(player);
    }

    private ArmorStand spawnStand(Location loc) {
        double pitch = service.getProperty(RestrictionSettings.HEAD_PITCH);
        double yaw = service.getProperty(RestrictionSettings.HEAD_YAW);
        Location location = new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getBlockZ(),
            (float) yaw, (float) pitch);

        ArmorStand stand = location.getWorld().spawn(location, ArmorStand.class);

        stand.setGravity(false);
        stand.setAI(false);
        stand.setInvisible(true);

        return stand;
    }

}
