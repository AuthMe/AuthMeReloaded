package fr.xephi.authme.data;

import fr.xephi.authme.initialization.HasCleanup;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerPermission;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.ProtectionSettings;
import fr.xephi.authme.util.expiring.ExpiringSet;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class QuickCommandsProtectionManager implements SettingsDependent, HasCleanup {

    private final PermissionsManager permissionsManager;

    private final ExpiringSet<String> latestJoin;

    @Inject
    public QuickCommandsProtectionManager(Settings settings, PermissionsManager permissionsManager) {
        this.permissionsManager = permissionsManager;
        long countTimeout = settings.getProperty(ProtectionSettings.QUICK_COMMANDS_DENIED_BEFORE_MILLISECONDS);
        latestJoin = new ExpiringSet<>(countTimeout, TimeUnit.MILLISECONDS);
        reload(settings);
    }

    /**
     * Save the player in the set
     * @param name the player's name
     */
    private void setJoin(String name) {
        latestJoin.add(name);
    }

    /**
     * Returns whether the given player has the permission and should be saved in the set
     * @param player the player to check
     * @return true if the player has the permission, false otherwise
     */
    private boolean shouldSavePlayer(Player player) {
        return permissionsManager.hasPermission(player, PlayerPermission.QUICK_COMMANDS_PROTECTION);
    }

    /**
     * Process the player join
     * @param player the player to process
     */
    public void processJoin(Player player) {
        if(shouldSavePlayer(player)) {
            setJoin(player.getName());
        }
    }

    /**
     * Returns whether the given player is able to perform the command
     * @param name the name of the player to check
     * @return true if the player is not in the set (so it's allowed to perform the command), false otherwise
     */
    public boolean isAllowed(String name) {
        return !latestJoin.contains(name);
    }

    @Override
    public void reload(Settings settings) {
        long countTimeout = settings.getProperty(ProtectionSettings.QUICK_COMMANDS_DENIED_BEFORE_MILLISECONDS);
        latestJoin.setExpiration(countTimeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public void performCleanup() {
        latestJoin.removeExpiredEntries();
    }
}
