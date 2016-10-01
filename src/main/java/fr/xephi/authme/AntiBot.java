package fr.xephi.authme;

import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.permission.AdminPermission;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.ProtectionSettings;
import fr.xephi.authme.util.BukkitService;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.concurrent.CopyOnWriteArrayList;

import static fr.xephi.authme.util.BukkitService.TICKS_PER_MINUTE;
import static fr.xephi.authme.util.BukkitService.TICKS_PER_SECOND;

/**
 * The AntiBot Service Management class.
 */
public class AntiBot {

    private final Settings settings;
    private final Messages messages;
    private final PermissionsManager permissionsManager;
    private final BukkitService bukkitService;
    private final CopyOnWriteArrayList<String> antibotKicked = new CopyOnWriteArrayList<String>();
    private final CopyOnWriteArrayList<String> antibotPlayers = new CopyOnWriteArrayList<String>();
    private AntiBotStatus antiBotStatus = AntiBotStatus.DISABLED;

    @Inject
    AntiBot(Settings settings, Messages messages, PermissionsManager permissionsManager,
            BukkitService bukkitService) {
        this.settings = settings;
        this.messages = messages;
        this.permissionsManager = permissionsManager;
        this.bukkitService = bukkitService;

        setupAntiBotService();
    }

    private void setupAntiBotService() {
        if (settings.getProperty(ProtectionSettings.ENABLE_ANTIBOT)) {
            bukkitService.scheduleSyncDelayedTask(new Runnable() {
                @Override
                public void run() {
                    antiBotStatus = AntiBotStatus.LISTENING;
                }
            }, 2 * TICKS_PER_MINUTE);
        }
    }

    public void overrideAntiBotStatus(boolean activated) {
        if (antiBotStatus != AntiBotStatus.DISABLED) {
            if (activated) {
                antiBotStatus = AntiBotStatus.ACTIVE;
            } else {
                antiBotStatus = AntiBotStatus.LISTENING;
            }
        }
    }

    public AntiBotStatus getAntiBotStatus() {
        return antiBotStatus;
    }

    public void activateAntiBot() {
        antiBotStatus = AntiBotStatus.ACTIVE;
        for(Player player : bukkitService.getOnlinePlayers()) {
            if(!permissionsManager.hasPermission(player, AdminPermission.ANTIBOT_MESSAGES)) {
                continue;
            }
            messages.send(player, MessageKey.ANTIBOT_AUTO_ENABLED_MESSAGE);
        }

        final int duration = settings.getProperty(ProtectionSettings.ANTIBOT_DURATION);
        bukkitService.scheduleSyncDelayedTask(new Runnable() {
            @Override
            public void run() {
                if (antiBotStatus == AntiBotStatus.ACTIVE) {
                    antiBotStatus = AntiBotStatus.LISTENING;
                    antibotPlayers.clear();
                    antibotKicked.clear();
                    for (String s : messages.retrieve(MessageKey.ANTIBOT_AUTO_DISABLED_MESSAGE)) {
                        bukkitService.broadcastMessage(s.replace("%m", Integer.toString(duration)));
                    }
                }
            }
        }, duration * TICKS_PER_MINUTE);
    }

    /**
     * Handles a player joining the server and checks if AntiBot needs to be activated.
     *
     * @param player the player who joined the server
     */
    public void handlePlayerJoin(final Player player) {
        if (antiBotStatus == AntiBotStatus.ACTIVE || antiBotStatus == AntiBotStatus.DISABLED) {
            return;
        }
        if (permissionsManager.hasPermission(player, PlayerStatePermission.BYPASS_ANTIBOT)) {
            return;
        }

        antibotPlayers.add(player.getName().toLowerCase());
        if (antibotPlayers.size() > settings.getProperty(ProtectionSettings.ANTIBOT_SENSIBILITY)) {
            activateAntiBot();
            return;
        }
        bukkitService.scheduleSyncDelayedTask(new Runnable() {
            @Override
            public void run() {
                antibotPlayers.remove(player.getName().toLowerCase());
            }
        }, 15 * TICKS_PER_SECOND);
    }

    /**
     * Returns whether the player was kicked because of activated antibot. The list is reset
     * when antibot is deactivated.
     *
     * @param name the name to check
     * @return true if the given name has been kicked because of Antibot
     */
    public boolean wasPlayerKicked(String name) {
        return antibotKicked.contains(name.toLowerCase());
    }

    /**
     * Adds a name to the list of players kicked by antibot. Should only be used when a player
     * is determined to be kicked because of failed antibot verification.
     *
     * @param name the name to add
     */
    public void addPlayerKick(String name) {
        antibotKicked.addIfAbsent(name.toLowerCase());
    }

    public enum AntiBotStatus {
        LISTENING,
        DISABLED,
        ACTIVE
    }

}
