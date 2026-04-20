package fr.xephi.authme.process.register.executors;

import org.bukkit.entity.Player;

/**
 * Parameters for email registration.
 */
public class EmailRegisterParams extends RegistrationParameters {

    private final String email;
    private String password;

    protected EmailRegisterParams(Player player, String email) {
        super(player);
        this.email = email;
    }

    /**
     * Creates a params object for email registration.
     *
     * @param player the player to register
     * @param email the player's email
     * @return params object with the given data
     */
    public static EmailRegisterParams of(Player player, String email) {
        return new EmailRegisterParams(player, email);
    }

    public String getEmail() {
        return email;
    }

    void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the password generated for the player
     */
    String getPassword() {
        return password;
    }
}
