package fr.xephi.authme.platform;

import fr.xephi.authme.process.register.RegisterSecondaryArgument;
import fr.xephi.authme.process.register.RegistrationType;
import org.bukkit.entity.Player;

/**
 * Platform adapter for showing graphical dialog UIs to players on supported platforms (1.21.6+).
 * The default implementation is a no-op (dialog not supported).
 */
public interface DialogAdapter {

    /**
     * Returns whether this platform supports showing dialog UIs.
     *
     * @return true if dialog UI is supported, false otherwise
     */
    default boolean isDialogSupported() {
        return false;
    }

    /**
     * Shows the login dialog to the given player.
     *
     * @param player the player to show the dialog to
     * @param dialog the resolved dialog text and UX options
     */
    default void showLoginDialog(Player player, DialogWindowSpec dialog) {
    }

    /**
     * Shows the two-factor authentication dialog to the given player.
     *
     * @param player the player to show the dialog to
     * @param dialog the resolved dialog text and UX options
     */
    default void showTotpDialog(Player player, DialogWindowSpec dialog) {
    }

    /**
     * Shows the register dialog to the given player.
     *
     * @param player the player to show the dialog to
     * @param type the registration type (password or email)
     * @param secondArg the secondary argument type for registration
     * @param dialog the resolved dialog text and UX options
     */
    default void showRegisterDialog(Player player, RegistrationType type,
                                    RegisterSecondaryArgument secondArg, DialogWindowSpec dialog) {
    }

    /**
     * Closes any open dialog for the given player (e.g. after a force-login via the API).
     * Has no effect if no dialog is currently open or if dialogs are not supported on this platform.
     *
     * @param player the player whose dialog should be closed
     */
    default void closeDialog(Player player) {
    }
}
