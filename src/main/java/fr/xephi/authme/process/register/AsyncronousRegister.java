package fr.xephi.authme.process.register;

import java.security.NoSuchAlgorithmException;
import java.util.Date;

import org.bukkit.entity.Player;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;

public class AsyncronousRegister {

    protected Player player;
    protected String name;
    protected String password;
    protected String email = "";
    protected boolean allowRegister;
    private AuthMe plugin;
    private DataSource database;
    private Messages m = Messages.getInstance();

    public AsyncronousRegister(Player player, String password, String email,
            AuthMe plugin, DataSource data) {
        this.player = player;
        this.password = password;
        name = player.getName().toLowerCase();
        this.email = email;
        this.plugin = plugin;
        this.database = data;
        this.allowRegister = true;
    }

    protected String getIp() {
        return plugin.getIP(player);
    }

    protected void preRegister() {
        if (PlayerCache.getInstance().isAuthenticated(name)) {
            m.send(player, "logged_in");
            allowRegister = false;
        }

        if (!Settings.isRegistrationEnabled) {
            m.send(player, "reg_disabled");
            allowRegister = false;
        }

        String lowpass = password.toLowerCase();
        if ((lowpass.contains("delete") || lowpass.contains("where") || lowpass.contains("insert") || lowpass.contains("modify") || lowpass.contains("from") || lowpass.contains("select") || lowpass.contains(";") || lowpass.contains("null")) || !lowpass.matches(Settings.getPassRegex)) {
            m.send(player, "password_error");
            allowRegister = false;
        }

        if (database.isAuthAvailable(name)) {
            m.send(player, "user_regged");
            if (plugin.pllog.getStringList("players").contains(name)) {
                plugin.pllog.getStringList("players").remove(name);
            }
            allowRegister = false;
        }

        if (Settings.getmaxRegPerIp > 0) {
            if (!plugin.authmePermissible(player, "authme.allow2accounts") && database.getAllAuthsByIp(getIp()).size() >= Settings.getmaxRegPerIp && !getIp().equalsIgnoreCase("127.0.0.1") && !getIp().equalsIgnoreCase("localhost")) {
                m.send(player, "max_reg");
                allowRegister = false;
            }
        }

    }

    public void process() {
        preRegister();
        if (!allowRegister)
            return;
        if (!email.isEmpty() && email != "") {
            if (Settings.getmaxRegPerEmail > 0) {
                if (!plugin.authmePermissible(player, "authme.allow2accounts") && database.getAllAuthsByEmail(email).size() >= Settings.getmaxRegPerEmail) {
                    m.send(player, "max_reg");
                    return;
                }
            }
            emailRegister();
            return;
        }
        passwordRegister();
    }

    protected void emailRegister() {
        if (Settings.getmaxRegPerEmail > 0) {
            if (!plugin.authmePermissible(player, "authme.allow2accounts") && database.getAllAuthsByEmail(email).size() >= Settings.getmaxRegPerEmail) {
                m.send(player, "max_reg");
                return;
            }
        }
        PlayerAuth auth = null;
        try {
            final String hashnew = PasswordSecurity.getHash(Settings.getPasswordHash, password, name);
            auth = new PlayerAuth(name, hashnew, getIp(), 0, (int) player.getLocation().getX(), (int) player.getLocation().getY(), (int) player.getLocation().getZ(), player.getLocation().getWorld().getName(), email);
        } catch (NoSuchAlgorithmException e) {
            ConsoleLogger.showError(e.getMessage());
            m.send(player, "error");
            return;
        }
        if (PasswordSecurity.userSalt.containsKey(name)) {
            auth.setSalt(PasswordSecurity.userSalt.get(name));
        }
        database.saveAuth(auth);
        database.updateEmail(auth);
        database.updateSession(auth);
        plugin.mail.main(auth, password);
        ProcessSyncronousEmailRegister syncronous = new ProcessSyncronousEmailRegister(player, plugin);
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, syncronous);
        return;
    }

    protected void passwordRegister() {
        if (password.length() < Settings.getPasswordMinLen || password.length() > Settings.passwordMaxLength) {
            m.send(player, "pass_len");
            return;
        }
        if (!Settings.unsafePasswords.isEmpty()) {
            if (Settings.unsafePasswords.contains(password.toLowerCase())) {
                m.send(player, "password_error");
                return;
            }
        }
        PlayerAuth auth = null;
        String hash = "";
        try {
            hash = PasswordSecurity.getHash(Settings.getPasswordHash, password, name);
        } catch (NoSuchAlgorithmException e) {
            ConsoleLogger.showError(e.getMessage());
            m.send(player, "error");
            return;
        }
        if (Settings.getMySQLColumnSalt.isEmpty() && !PasswordSecurity.userSalt.containsKey(name)) {
            auth = new PlayerAuth(name, hash, getIp(), new Date().getTime(), "your@email.com");
        } else {
            auth = new PlayerAuth(name, hash, PasswordSecurity.userSalt.get(name), getIp(), new Date().getTime());
        }
        if (!database.saveAuth(auth)) {
            m.send(player, "error");
            return;
        }
        if (!Settings.forceRegLogin) {
            PlayerCache.getInstance().addPlayer(auth);
            database.setLogged(name);
        }
        plugin.otherAccounts.addPlayer(player.getUniqueId());
        ProcessSyncronousPasswordRegister syncronous = new ProcessSyncronousPasswordRegister(player, plugin);
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, syncronous);
        return;
    }
}
