package fr.xephi.authme.process.register.executors;

import org.bukkit.entity.Player;

/**
 * Parameters for registration with a given password, and optionally an email address.
 */
public class PasswordRegisterParams extends AbstractPasswordRegisterParams {

    private final String email;

    protected PasswordRegisterParams(Player player, String password, String email) {
        super(player, password);
        this.email = email;
    }

    /**
     * Creates a params object.
     *
     * @param player the player to register
     * @param password the password to register with
     * @param email the email of the player (may be null)
     * @return params object with the given data
     */
    public static PasswordRegisterParams of(Player player, String password, String email) {
        return new PasswordRegisterParams(player, password, email);
    }

    public String getEmail() {
        return email;
    }
}
