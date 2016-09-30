package fr.xephi.authme.process.register;

import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.mail.SendMailSSL;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.process.AsynchronousProcess;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.process.SyncProcessManager;
import fr.xephi.authme.process.login.AsynchronousLogin;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.security.crypts.TwoFactor;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.StringUtils;
import fr.xephi.authme.util.Utils;
import fr.xephi.authme.util.ValidationService;
import fr.xephi.authme.util.ValidationService.ValidationResult;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

import static fr.xephi.authme.permission.PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS;

/**
 * Asynchronous processing of a request for registration.
 */
public class AsyncRegister implements AsynchronousProcess {

    /**
     * Number of ticks to wait before running the login action when it is run synchronously.
     * A small delay is necessary or the database won't return the newly saved PlayerAuth object
     * and the login process thinks the user is not registered.
     */
    private static final int SYNC_LOGIN_DELAY = 5;

    @Inject
    private DataSource database;
    @Inject
    private PlayerCache playerCache;
    @Inject
    private PasswordSecurity passwordSecurity;
    @Inject
    private ProcessService service;
    @Inject
    private SyncProcessManager syncProcessManager;
    @Inject
    private PermissionsManager permissionsManager;
    @Inject
    private ValidationService validationService;
    @Inject
    private SendMailSSL sendMailSsl;
    @Inject
    private AsynchronousLogin asynchronousLogin;
    @Inject
    private BukkitService bukkitService;

    AsyncRegister() {
    }

    private boolean preRegisterCheck(Player player, String password) {
        final String name = player.getName().toLowerCase();
        if (playerCache.isAuthenticated(name)) {
            service.send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
            return false;
        } else if (!service.getProperty(RegistrationSettings.IS_ENABLED)) {
            service.send(player, MessageKey.REGISTRATION_DISABLED);
            return false;
        }

        //check the password safety only if it's not a automatically generated password
        if (service.getProperty(SecuritySettings.PASSWORD_HASH) != HashAlgorithm.TWO_FACTOR) {
            ValidationResult passwordValidation = validationService.validatePassword(password, player.getName());
            if (passwordValidation.hasError()) {
                service.send(player, passwordValidation.getMessageKey(), passwordValidation.getArgs());
                return false;
            }
        }

        //check this in both possibilities so don't use 'else if'
        if (database.isAuthAvailable(name)) {
            service.send(player, MessageKey.NAME_ALREADY_REGISTERED);
            return false;
        }

        final int maxRegPerIp = service.getProperty(RestrictionSettings.MAX_REGISTRATION_PER_IP);
        final String ip = Utils.getPlayerIp(player);
        if (maxRegPerIp > 0
            && !"127.0.0.1".equalsIgnoreCase(ip)
            && !"localhost".equalsIgnoreCase(ip)
            && !permissionsManager.hasPermission(player, ALLOW_MULTIPLE_ACCOUNTS)) {
            List<String> otherAccounts = database.getAllAuthsByIp(ip);
            if (otherAccounts.size() >= maxRegPerIp) {
                service.send(player, MessageKey.MAX_REGISTER_EXCEEDED, Integer.toString(maxRegPerIp),
                    Integer.toString(otherAccounts.size()), StringUtils.join(", ", otherAccounts));
                return false;
            }
        }
        return true;
    }

    public void register(Player player, String password, String email, boolean autoLogin) {
        if (preRegisterCheck(player, password)) {
            if (!StringUtils.isEmpty(email)) {
                emailRegister(player, password, email);
            } else {
                passwordRegister(player, password, autoLogin);
            }
        }
    }

    private void emailRegister(Player player, String password, String email) {
        final String name = player.getName().toLowerCase();
        final int maxRegPerEmail = service.getProperty(EmailSettings.MAX_REG_PER_EMAIL);
        if (maxRegPerEmail > 0 && !permissionsManager.hasPermission(player, ALLOW_MULTIPLE_ACCOUNTS)) {
            int otherAccounts = database.countAuthsByEmail(email);
            if (otherAccounts >= maxRegPerEmail) {
                service.send(player, MessageKey.MAX_REGISTER_EXCEEDED, Integer.toString(maxRegPerEmail),
                    Integer.toString(otherAccounts), "@");
                return;
            }
        }

        final HashedPassword hashedPassword = passwordSecurity.computeHash(password, name);
        final String ip = Utils.getPlayerIp(player);
        PlayerAuth auth = PlayerAuth.builder()
            .name(name)
            .realName(player.getName())
            .password(hashedPassword)
            .ip(ip)
            .location(player.getLocation())
            .email(email)
            .build();

        if (!database.saveAuth(auth)) {
            service.send(player, MessageKey.ERROR);
            return;
        }
        database.updateEmail(auth);
        database.updateSession(auth);
        sendMailSsl.sendPasswordMail(name, email, password);
        syncProcessManager.processSyncEmailRegister(player);
    }

    private void passwordRegister(final Player player, String password, boolean autoLogin) {
        final String name = player.getName().toLowerCase();
        final String ip = Utils.getPlayerIp(player);
        final HashedPassword hashedPassword = passwordSecurity.computeHash(password, name);
        PlayerAuth auth = PlayerAuth.builder()
            .name(name)
            .realName(player.getName())
            .password(hashedPassword)
            .ip(ip)
            .location(player.getLocation())
            .build();

        if (!database.saveAuth(auth)) {
            service.send(player, MessageKey.ERROR);
            return;
        }

        if (!service.getProperty(RegistrationSettings.FORCE_LOGIN_AFTER_REGISTER) && autoLogin) {
            if (service.getProperty(PluginSettings.USE_ASYNC_TASKS)) {
                bukkitService.runTaskAsynchronously(() -> asynchronousLogin.forceLogin(player));
            } else {
                bukkitService.scheduleSyncDelayedTask(() -> asynchronousLogin.forceLogin(player), SYNC_LOGIN_DELAY);
            }
        }
        syncProcessManager.processSyncPasswordRegister(player);

        //give the user the secret code to setup their app code generation
        if (service.getProperty(SecuritySettings.PASSWORD_HASH) == HashAlgorithm.TWO_FACTOR) {
            String qrCodeUrl = TwoFactor.getQRBarcodeURL(player.getName(), Bukkit.getIp(), hashedPassword.getHash());
            service.send(player, MessageKey.TWO_FACTOR_CREATE, hashedPassword.getHash(), qrCodeUrl);
        }
    }
}
