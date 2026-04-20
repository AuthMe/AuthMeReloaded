package fr.xephi.authme.process.register.executors;

import org.bukkit.entity.Player;

/**
 * Parameters for {@link ApiPasswordRegisterExecutor}.
 */
public class ApiPasswordRegisterParams extends PasswordRegisterParams {

    private final boolean loginAfterRegister;

    protected ApiPasswordRegisterParams(Player player, String password, boolean loginAfterRegister) {
        super(player, password, null);
        this.loginAfterRegister = loginAfterRegister;
    }

    /**
     * Creates a parameters object.
     *
     * @param player the player to register
     * @param password the password to register with
     * @param loginAfterRegister whether the player should be logged in after registration
     * @return params object with the given data
     */
    public static ApiPasswordRegisterParams of(Player player, String password, boolean loginAfterRegister) {
        return new ApiPasswordRegisterParams(player, password, loginAfterRegister);
    }

    /**
     * @return true if the player should be logged in after being registered, false otherwise
     */
    public boolean getLoginAfterRegister() {
        return loginAfterRegister;
    }
}
