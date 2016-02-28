package fr.xephi.authme.process.register;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.security.crypts.TwoFactor;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.StringUtils;

/**
 */
public class AsyncRegister {

    private final Player player;
    private final String name;
    private final String password;
    private final String ip;
    private final String email;
    private final AuthMe plugin;
    private final DataSource database;
    private final Messages m;
    private final NewSetting settings;

    public AsyncRegister(Player player, String password, String email, AuthMe plugin, DataSource data,
                         NewSetting settings) {
        this.m = plugin.getMessages();
        this.player = player;
        this.password = password;
        this.name = player.getName().toLowerCase();
        this.email = email;
        this.plugin = plugin;
        this.database = data;
        this.ip = plugin.getIP(player);
        this.settings = settings;
    }

    private boolean preRegisterCheck() {
        String passLow = password.toLowerCase();
        if (PlayerCache.getInstance().isAuthenticated(name)) {
            m.send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
            return false;
        } else if (!Settings.isRegistrationEnabled) {
            m.send(player, MessageKey.REGISTRATION_DISABLED);
            return false;
        }

        //check the password safety only if it's not a automatically generated password
        if (Settings.getPasswordHash != HashAlgorithm.TWO_FACTOR) {
            if (!passLow.matches(Settings.getPassRegex)) {
                m.send(player, MessageKey.PASSWORD_MATCH_ERROR);
                return false;
            } else if (passLow.equalsIgnoreCase(player.getName())) {
                m.send(player, MessageKey.PASSWORD_IS_USERNAME_ERROR);
                return false;
            } else if (password.length() < Settings.getPasswordMinLen || password.length() > Settings.passwordMaxLength) {
                m.send(player, MessageKey.INVALID_PASSWORD_LENGTH);
                return false;
            } else if (!Settings.unsafePasswords.isEmpty() && Settings.unsafePasswords.contains(password.toLowerCase())) {
                m.send(player, MessageKey.PASSWORD_UNSAFE_ERROR);
                return false;
            }
        }

        //check this in both possiblities so don't use 'else if'
        Integer size = 0;
        if (database.isAuthAvailable(name)) {
            m.send(player, MessageKey.NAME_ALREADY_REGISTERED);
            return false;
        } else if (Settings.getmaxRegPerIp > 0
            && !plugin.getPermissionsManager().hasPermission(player, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS)
            && !ip.equalsIgnoreCase("127.0.0.1")
            && !ip.equalsIgnoreCase("localhost")
            && (size = database.getAllAuthsByIp(ip).size()) >= Settings.getmaxRegPerIp) {
            m.send(player, MessageKey.MAX_REGISTER_EXCEEDED, size.toString());
            return false;
        }
        return true;
    }

    public void process() {
        if (preRegisterCheck()) {
            if (!StringUtils.isEmpty(email)) {
                emailRegister();
            } else {
                passwordRegister();
            }
        }
    }

    private void emailRegister() {
        Integer size = 0;
        if (Settings.getmaxRegPerEmail > 0
            && !plugin.getPermissionsManager().hasPermission(player, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS)
            && (size = database.countAuthsByEmail(email)) >= Settings.getmaxRegPerEmail) {
            m.send(player, MessageKey.MAX_REGISTER_EXCEEDED, size.toString());
            return;
        }
        final HashedPassword hashedPassword = plugin.getPasswordSecurity().computeHash(password, name);
        PlayerAuth auth = PlayerAuth.builder()
            .name(name)
            .realName(player.getName())
            .password(hashedPassword)
            .ip(ip)
            .location(player.getLocation())
            .email(email)
            .build();

        if (!database.saveAuth(auth)) {
            m.send(player, MessageKey.ERROR);
            return;
        }
        database.updateEmail(auth);
        database.updateSession(auth);
        plugin.mail.main(auth, password);
        ProcessSyncEmailRegister sync = new ProcessSyncEmailRegister(player, plugin);
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, sync);

    }

    private void passwordRegister() {
        final HashedPassword hashedPassword = plugin.getPasswordSecurity().computeHash(password, name);
        PlayerAuth auth = PlayerAuth.builder()
            .name(name)
            .realName(player.getName())
            .password(hashedPassword)
            .ip(ip)
            .location(player.getLocation())
            .build();

        if (!database.saveAuth(auth)) {
            m.send(player, MessageKey.ERROR);
            return;
        }

        if (!Settings.forceRegLogin) {
            //PlayerCache.getInstance().addPlayer(auth);
            //database.setLogged(name);
            // TODO: check this...
            plugin.getManagement().performLogin(player, "dontneed", true);
        }

        plugin.otherAccounts.addPlayer(player.getUniqueId());
        ProcessSyncPasswordRegister sync = new ProcessSyncPasswordRegister(player, plugin, settings);
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, sync);

        //give the user the secret code to setup their app code generation
        if (Settings.getPasswordHash == HashAlgorithm.TWO_FACTOR) {
            String qrCodeUrl = TwoFactor.getQRBarcodeURL(player.getName(), Bukkit.getIp(), hashedPassword.getHash());
            m.send(player, MessageKey.TWO_FACTOR_CREATE, hashedPassword.getHash(), qrCodeUrl);
        }
    }
}
