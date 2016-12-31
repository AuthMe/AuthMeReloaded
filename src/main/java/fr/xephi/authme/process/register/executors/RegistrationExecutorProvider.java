package fr.xephi.authme.process.register.executors;

import org.bukkit.entity.Player;

import javax.inject.Inject;

/**
 * Provides a {@link RegistrationExecutor} for various registration methods.
 */
public class RegistrationExecutorProvider {

    @Inject
    private PasswordRegisterExecutorProvider passwordRegisterExecutorProvider;

    @Inject
    private EmailRegisterExecutorProvider emailRegisterExecutorProvider;

    RegistrationExecutorProvider() {
    }

    public RegistrationExecutor getPasswordRegisterExecutor(Player player, String password, String email) {
        return passwordRegisterExecutorProvider.new PasswordRegisterExecutor(player, password, email);
    }

    public RegistrationExecutor getPasswordRegisterExecutor(Player player, String password, boolean loginAfterRegister) {
        return passwordRegisterExecutorProvider.new ApiPasswordRegisterExecutor(player, password, loginAfterRegister);
    }

    public RegistrationExecutor getTwoFactorRegisterExecutor(Player player) {
        return passwordRegisterExecutorProvider.new TwoFactorRegisterExecutor(player);
    }

    public RegistrationExecutor getEmailRegisterExecutor(Player player, String email) {
        return emailRegisterExecutorProvider.new EmailRegisterExecutor(player, email);
    }
}
