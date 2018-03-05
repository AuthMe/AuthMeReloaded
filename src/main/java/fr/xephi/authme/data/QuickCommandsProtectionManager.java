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

    private final ExpiringSet<String> latestLogin;

    @Inject
    public QuickCommandsProtectionManager(Settings settings, PermissionsManager permissionsManager) {
        this.permissionsManager = permissionsManager;
        long countTimeout = settings.getProperty(ProtectionSettings.QUICK_COMMANDS_DENIED_BEFORE_MILLISECONDS);
        latestLogin = new ExpiringSet<>(countTimeout, TimeUnit.MILLISECONDS);
        reload(settings);
    }

    public void setLogin(String name) {
        latestLogin.add(name);
    }

    public boolean shouldSaveLogin(Player player) {
        return permissionsManager.hasPermission(player, PlayerPermission.QUICK_COMMANDS_PROTECTION);
    }

    public boolean isAllowed(String name) {
        return !latestLogin.contains(name);
    }

    @Override
    public void reload(Settings settings) {
        long countTimeout = settings.getProperty(ProtectionSettings.QUICK_COMMANDS_DENIED_BEFORE_MILLISECONDS);
        latestLogin.setExpiration(countTimeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public void performCleanup() {
        latestLogin.removeExpiredEntries();
    }
}
