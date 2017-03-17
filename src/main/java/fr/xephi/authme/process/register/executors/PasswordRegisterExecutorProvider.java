package fr.xephi.authme.process.register.executors;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.SyncProcessManager;
import fr.xephi.authme.process.login.AsynchronousLogin;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.security.crypts.TwoFactor;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.service.ValidationService.ValidationResult;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.inject.Inject;

import static fr.xephi.authme.process.register.executors.PlayerAuthBuilderHelper.createPlayerAuth;

/**
 * Provides registration executors for password-based registration variants.
 */
class PasswordRegisterExecutorProvider {

    /**
     * Number of ticks to wait before running the login action when it is run synchronously.
     * A small delay is necessary or the database won't return the newly saved PlayerAuth object
     * and the login process thinks the user is not registered.
     */
    private static final int SYNC_LOGIN_DELAY = 5;

    @Inject
    private ValidationService validationService;

    @Inject
    private CommonService commonService;

    @Inject
    private PasswordSecurity passwordSecurity;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private SyncProcessManager syncProcessManager;

    @Inject
    private AsynchronousLogin asynchronousLogin;

    PasswordRegisterExecutorProvider() {
    }

    /** Registration executor for password registration. */
    class PasswordRegisterExecutor implements RegistrationExecutor {

        private final Player player;
        private final String password;
        private final String email;
        private HashedPassword hashedPassword;

        /**
         * Constructor.
         *
         * @param player the player to register
         * @param password the password to register with
         * @param email the email of the player (may be null)
         */
        PasswordRegisterExecutor(Player player, String password, String email) {
            this.player = player;
            this.password = password;
            this.email = email;
        }

        @Override
        public boolean isRegistrationAdmitted() {
            ValidationResult passwordValidation = validationService.validatePassword(password, player.getName());
            if (passwordValidation.hasError()) {
                commonService.send(player, passwordValidation.getMessageKey(), passwordValidation.getArgs());
                return false;
            }
            return true;
        }

        @Override
        public PlayerAuth buildPlayerAuth() {
            hashedPassword = passwordSecurity.computeHash(password, player.getName().toLowerCase());
            return createPlayerAuth(player, hashedPassword, email);
        }

        protected boolean performLoginAfterRegister() {
            return !commonService.getProperty(RegistrationSettings.FORCE_LOGIN_AFTER_REGISTER);
        }

        @Override
        public void executePostPersistAction() {
            if (performLoginAfterRegister()) {
                if (commonService.getProperty(PluginSettings.USE_ASYNC_TASKS)) {
                    bukkitService.runTaskAsynchronously(() -> asynchronousLogin.forceLogin(player));
                } else {
                    bukkitService.scheduleSyncDelayedTask(() -> asynchronousLogin.forceLogin(player), SYNC_LOGIN_DELAY);
                }
            }
            syncProcessManager.processSyncPasswordRegister(player);
        }

        protected Player getPlayer() {
            return player;
        }

        protected HashedPassword getHashedPassword() {
            return hashedPassword;
        }
    }

    /** Executor for password registration via API call. */
    class ApiPasswordRegisterExecutor extends PasswordRegisterExecutor {

        private final boolean loginAfterRegister;

        /**
         * Constructor.
         *
         * @param player the player to register
         * @param password the password to register with
         * @param loginAfterRegister whether the user should be automatically logged in after registration
         */
        ApiPasswordRegisterExecutor(Player player, String password, boolean loginAfterRegister) {
            super(player, password, null);
            this.loginAfterRegister = loginAfterRegister;
        }

        @Override
        protected boolean performLoginAfterRegister() {
            return loginAfterRegister;
        }
    }

    /** Executor for two factor registration. */
    class TwoFactorRegisterExecutor extends PasswordRegisterExecutor {

        TwoFactorRegisterExecutor(Player player) {
            super(player, "", null);
        }

        @Override
        public boolean isRegistrationAdmitted() {
            // nothing to check
            return true;
        }

        @Override
        public void executePostPersistAction() {
            super.executePostPersistAction();

            String hash = getHashedPassword().getHash();
            String qrCodeUrl = TwoFactor.getQRBarcodeURL(getPlayer().getName(), Bukkit.getIp(), hash);
            commonService.send(getPlayer(), MessageKey.TWO_FACTOR_CREATE, hash, qrCodeUrl);
        }

    }
}
