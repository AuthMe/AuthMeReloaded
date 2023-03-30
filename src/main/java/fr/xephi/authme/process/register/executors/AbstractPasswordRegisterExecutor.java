package fr.xephi.authme.process.register.executors;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.process.SyncProcessManager;
import fr.xephi.authme.process.login.AsynchronousLogin;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import org.bukkit.entity.Player;

import javax.inject.Inject;

/**
 * Registration executor for registration methods where the password
 * is supplied by the user.
 *
 * @param <P> the parameters type
 */
abstract class AbstractPasswordRegisterExecutor<P extends AbstractPasswordRegisterParams>
    implements RegistrationExecutor<P> {

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

    @Override
    public boolean isRegistrationAdmitted(P params) {
        ValidationService.ValidationResult passwordValidation = validationService.validatePassword(
            params.getPassword(), params.getPlayer().getName());
        if (passwordValidation.hasError()) {
            commonService.send(params.getPlayer(), passwordValidation.getMessageKey(), passwordValidation.getArgs());
            return false;
        }
        return true;
    }

    @Override
    public PlayerAuth buildPlayerAuth(P params) {
        HashedPassword hashedPassword = passwordSecurity.computeHash(params.getPassword(), params.getPlayerName());
        params.setHashedPassword(hashedPassword);
        return createPlayerAuthObject(params);
    }

    /**
     * Creates the PlayerAuth object to store into the database, based on the registration parameters.
     *
     * @param params the parameters
     * @return the PlayerAuth representing the new account to register
     */
    protected abstract PlayerAuth createPlayerAuthObject(P params);

    /**
     * Returns whether the player should be automatically logged in after registration.
     *
     * @param params the registration parameters
     * @return true if the player should be logged in, false otherwise
     */
    protected boolean performLoginAfterRegister(P params) {
        return !commonService.getProperty(RegistrationSettings.FORCE_LOGIN_AFTER_REGISTER);
    }

    @Override
    public void executePostPersistAction(P params) {
        final Player player = params.getPlayer();
        if (performLoginAfterRegister(params)) {
            if (commonService.getProperty(PluginSettings.USE_ASYNC_TASKS)) {
                bukkitService.runOnAsyncSchedulerNow(task -> asynchronousLogin.forceLogin(player));
            } else {
                bukkitService.runOnGlobalRegionSchedulerDelayed(task -> asynchronousLogin.forceLogin(player), SYNC_LOGIN_DELAY);
            }
        }
        syncProcessManager.processSyncPasswordRegister(player);
    }
}
