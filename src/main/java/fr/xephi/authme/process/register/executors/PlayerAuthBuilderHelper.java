package fr.xephi.authme.process.register.executors;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.util.PlayerUtils;
import org.bukkit.entity.Player;

/**
 * Helper for constructing PlayerAuth objects.
 */
final class PlayerAuthBuilderHelper {

    static PlayerAuth createPlayerAuth(Player player, HashedPassword hashedPassword, String email) {
        return PlayerAuth.builder()
            .name(player.getName().toLowerCase())
            .realName(player.getName())
            .password(hashedPassword)
            .email(email)
            .ip(PlayerUtils.getPlayerIp(player))
            .location(player.getLocation())
            .build();
    }



}
