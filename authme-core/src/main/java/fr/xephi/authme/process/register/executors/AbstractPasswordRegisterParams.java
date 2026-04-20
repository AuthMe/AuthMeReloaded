package fr.xephi.authme.process.register.executors;

import fr.xephi.authme.security.crypts.HashedPassword;
import org.bukkit.entity.Player;

/**
 * Common params type for implementors of {@link AbstractPasswordRegisterExecutor}.
 * Password must be supplied on creation and cannot be changed later on. The {@link HashedPassword}
 * is stored on the params object for later use.
 */
public abstract class AbstractPasswordRegisterParams extends RegistrationParameters {

    private final String password;
    private HashedPassword hashedPassword;

    /**
     * Constructor.
     *
     * @param player the player to register
     * @param password the password to use
     */
    public AbstractPasswordRegisterParams(Player player, String password) {
        super(player);
        this.password = password;
    }

    /**
     * Constructor with no defined password. Use for registration methods which
     * have no implicit password (like two factor authentication).
     *
     * @param player the player to register
     */
    public AbstractPasswordRegisterParams(Player player) {
        this(player, null);
    }

    public String getPassword() {
        return password;
    }

    void setHashedPassword(HashedPassword hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    HashedPassword getHashedPassword() {
        return hashedPassword;
    }
}
