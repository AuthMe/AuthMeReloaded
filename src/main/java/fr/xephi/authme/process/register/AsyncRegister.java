package fr.xephi.authme.process.register;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.process.Process;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.security.crypts.TwoFactor;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

/**
 */
public class AsyncRegister implements Process {

    private final Player player;
    private final String name;
    private final String password;
    private final String ip;
    private final String email;
    private final AuthMe plugin;
    private final DataSource database;
    private final ProcessService service;

    public AsyncRegister(Player player, String password, String email, AuthMe plugin, DataSource data,
                         ProcessService service) {
        this.player = player;
        this.password = password;
        this.name = player.getName().toLowerCase();
        this.email = email;
        this.plugin = plugin;
        this.database = data;
        this.ip = service.getIpAddressManager().getPlayerIp(player);
        this.service = service;
    }

    private boolean preRegisterCheck() {
        String passLow = password.toLowerCase();
        if (PlayerCache.getInstance().isAuthenticated(name)) {
            service.send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
            return false;
        } else if (!Settings.isRegistrationEnabled) {
            service.send(player, MessageKey.REGISTRATION_DISABLED);
            return false;
        }

        //check the password safety only if it's not a automatically generated password
        if (service.getProperty(SecuritySettings.PASSWORD_HASH) != HashAlgorithm.TWO_FACTOR) {
            if (!passLow.matches(Settings.getPassRegex)) {
                service.send(player, MessageKey.PASSWORD_MATCH_ERROR);
                return false;
            } else if (passLow.equalsIgnoreCase(player.getName())) {
                service.send(player, MessageKey.PASSWORD_IS_USERNAME_ERROR);
                return false;
            } else if (password.length() < Settings.getPasswordMinLen || password.length() > Settings.passwordMaxLength) {
                service.send(player, MessageKey.INVALID_PASSWORD_LENGTH);
                return false;
            } else if (!Settings.unsafePasswords.isEmpty() && Settings.unsafePasswords.contains(password.toLowerCase())) {
                service.send(player, MessageKey.PASSWORD_UNSAFE_ERROR);
                return false;
            }
        }

        //check this in both possibilities so don't use 'else if'
        if (database.isAuthAvailable(name)) {
            service.send(player, MessageKey.NAME_ALREADY_REGISTERED);
            return false;
        } else if(Settings.getmaxRegPerIp > 0
            && !ip.equalsIgnoreCase("127.0.0.1")
            && !ip.equalsIgnoreCase("localhost")
            && !plugin.getPermissionsManager().hasPermission(player, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS)) {
            int maxReg = Settings.getmaxRegPerIp;
            List<String> otherAccounts = database.getAllAuthsByIp(ip);
            if (otherAccounts.size() >= maxReg) {
                service.send(player, MessageKey.MAX_REGISTER_EXCEEDED, Integer.toString(maxReg),
                    Integer.toString(otherAccounts.size()), StringUtils.join(", ", otherAccounts.toString()));
                return false;
            }
        }
        return true;
    }

    @Override
    public void run() {
        if (preRegisterCheck()) {
            if (!StringUtils.isEmpty(email)) {
                emailRegister();
            } else {
                passwordRegister();
            }
        }
    }

    private void emailRegister() {
        if (Settings.getmaxRegPerEmail > 0
            && !plugin.getPermissionsManager().hasPermission(player, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS)) {
            int maxReg = Settings.getmaxRegPerEmail;
            int otherAccounts = database.countAuthsByEmail(email);
            if (otherAccounts >= maxReg) {
                service.send(player, MessageKey.MAX_REGISTER_EXCEEDED, Integer.toString(maxReg),
                    Integer.toString(otherAccounts), "@");
                return;
            }
        }

        final HashedPassword hashedPassword = service.computeHash(password, name);
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
        plugin.mail.main(auth, password);
        ProcessSyncEmailRegister sync = new ProcessSyncEmailRegister(player, service);
        service.scheduleSyncDelayedTask(sync);

    }

    private void passwordRegister() {
        final HashedPassword hashedPassword = service.computeHash(password, name);
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

        if (!Settings.forceRegLogin) {
            //PlayerCache.getInstance().addPlayer(auth);
            //database.setLogged(name);
            // TODO: check this...
            plugin.getManagement().performLogin(player, "dontneed", true);
        }

        ProcessSyncPasswordRegister sync = new ProcessSyncPasswordRegister(player, plugin, service);
        service.scheduleSyncDelayedTask(sync);

        //give the user the secret code to setup their app code generation
        if (service.getProperty(SecuritySettings.PASSWORD_HASH) == HashAlgorithm.TWO_FACTOR) {
            String qrCodeUrl = TwoFactor.getQRBarcodeURL(player.getName(), Bukkit.getIp(), hashedPassword.getHash());
            service.send(player, MessageKey.TWO_FACTOR_CREATE, hashedPassword.getHash(), qrCodeUrl);
        }
    }
}
