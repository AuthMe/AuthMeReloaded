package fr.xephi.authme.process.register;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.permission.PlayerPermission;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.settings.MessageKey;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import org.bukkit.entity.Player;

import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 */
public class AsyncRegister {

    protected final Player player;
    protected final String name;
    protected final String password;
    protected String email = "";
    private final AuthMe plugin;
    private final DataSource database;
    private final Messages m;

    public AsyncRegister(Player player, String password, String email, AuthMe plugin, DataSource data) {
        this.m = plugin.getMessages();
        this.player = player;
        this.password = password;
        this.name = player.getName().toLowerCase();
        this.email = email;
        this.plugin = plugin;
        this.database = data;
    }

    protected String getIp() {
        return plugin.getIP(player);
    }

    protected boolean preRegisterCheck() throws Exception {
        String passLow = password.toLowerCase();
        if (PlayerCache.getInstance().isAuthenticated(name)) {
            m.send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
            return false;
        } else if (!Settings.isRegistrationEnabled) {
            m.send(player, MessageKey.REGISTRATION_DISABLED);
            return false;
        } else if (passLow.contains("delete") || passLow.contains("where") || passLow.contains("insert") || passLow.contains("modify") || passLow.contains("from") || passLow.contains("select") || passLow.contains(";") || passLow.contains("null") || !passLow.matches(Settings.getPassRegex)) {
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
        } else if (database.isAuthAvailable(name)) {
            m.send(player, MessageKey.NAME_ALREADY_REGISTERED);
            return false;
        } else if (Settings.getmaxRegPerIp > 0
                && !plugin.getPermissionsManager().hasPermission(player, PlayerPermission.ALLOW_MULTIPLE_ACCOUNTS)
                && database.getAllAuthsByIp(getIp()).size() >= Settings.getmaxRegPerIp
                && !getIp().equalsIgnoreCase("127.0.0.1")
                && !getIp().equalsIgnoreCase("localhost")) {
            m.send(player, MessageKey.MAX_REGISTER_EXCEEDED);
            return false;
        }
        return true;
    }

    public void process() {
        try {
            if (!preRegisterCheck()) {
                return;
            }
            if (!email.isEmpty() && !email.equals("")) {
                if (Settings.getmaxRegPerEmail > 0
                        && !plugin.getPermissionsManager().hasPermission(player, PlayerPermission.ALLOW_MULTIPLE_ACCOUNTS)
                        && database.getAllAuthsByEmail(email).size() >= Settings.getmaxRegPerEmail) {
                    m.send(player, MessageKey.MAX_REGISTER_EXCEEDED);
                    return;
                }
                emailRegister();
                return;
            }
            passwordRegister();
        } catch (Exception e) {
            ConsoleLogger.showError(e.getMessage());
            ConsoleLogger.writeStackTrace(e);
            m.send(player, MessageKey.ERROR);
        }
    }

    protected void emailRegister() throws Exception {
        if (Settings.getmaxRegPerEmail > 0
                && !plugin.getPermissionsManager().hasPermission(player, PlayerPermission.ALLOW_MULTIPLE_ACCOUNTS)
                && database.getAllAuthsByEmail(email).size() >= Settings.getmaxRegPerEmail) {
            m.send(player, MessageKey.MAX_REGISTER_EXCEEDED);
            return;
        }
        PlayerAuth auth;
        final String hashNew = PasswordSecurity.getHash(Settings.getPasswordHash, password, name);
        auth = new PlayerAuth(name, hashNew, getIp(), 0, (int) player.getLocation().getX(), (int) player.getLocation().getY(), (int) player.getLocation().getZ(), player.getLocation().getWorld().getName(), email, player.getName());
        if (PasswordSecurity.userSalt.containsKey(name)) {
            auth.setSalt(PasswordSecurity.userSalt.get(name));
        }
        database.saveAuth(auth);
        database.updateEmail(auth);
        database.updateSession(auth);
        plugin.mail.main(auth, password);
        ProcessSyncEmailRegister sync = new ProcessSyncEmailRegister(player, plugin);
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, sync);

    }

    protected void passwordRegister() {
        PlayerAuth auth;
        String hash;
        try {
            hash = PasswordSecurity.getHash(Settings.getPasswordHash, password, name);
        } catch (NoSuchAlgorithmException e) {
            ConsoleLogger.showError(e.getMessage());
            m.send(player, MessageKey.ERROR);
            return;
        }
        if (Settings.getMySQLColumnSalt.isEmpty() && !PasswordSecurity.userSalt.containsKey(name)) {
            auth = new PlayerAuth(name, hash, getIp(), new Date().getTime(), "your@email.com", player.getName());
        } else {
            auth = new PlayerAuth(name, hash, PasswordSecurity.userSalt.get(name), getIp(), new Date().getTime(), player.getName());
        }
        if (!database.saveAuth(auth)) {
            m.send(player, MessageKey.ERROR);
            return;
        }
        if (!Settings.forceRegLogin) {
            PlayerCache.getInstance().addPlayer(auth);
            database.setLogged(name);
        }
        plugin.otherAccounts.addPlayer(player.getUniqueId());
        ProcessSyncronousPasswordRegister sync = new ProcessSyncronousPasswordRegister(player, plugin);
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, sync);
    }
}
