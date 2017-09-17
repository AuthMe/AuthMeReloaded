package fr.xephi.authme.process.register.executors;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.util.PlayerUtils;
import org.bukkit.entity.Player;

/**
 * Helper for constructing PlayerAuth objects.
 */
final class PlayerAuthBuilderHelper {

    private PlayerAuthBuilderHelper() {
    }

    /**
     * Creates a {@link PlayerAuth} object with the given data.
     *
     * @param player the player to create a PlayerAuth for
     * @param hashedPassword the hashed password
     * @param email the email address (nullable)
     * @return the generated PlayerAuth object
     */
    static PlayerAuth createPlayerAuth(Player player, HashedPassword hashedPassword, String email) {
        String playerIp = PlayerUtils.getPlayerIp(player);
        return PlayerAuth.builder()
            .name(player.getName().toLowerCase())
            .realName(player.getName())
            .password(hashedPassword)
            .email(email)
            .registrationIp(playerIp)
            .location(player.getLocation())
            .build();
    }
}
