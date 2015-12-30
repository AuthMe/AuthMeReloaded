package fr.xephi.authme.process.register;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.permission.PlayerPermission;
import fr.xephi.authme.security.crypts.EncryptedPassword;
import fr.xephi.authme.settings.Settings;
import org.bukkit.entity.Player;

/**
 */
public class AsyncRegister {

    protected final Player player;
    protected final String name;
    protected final String password;
    private final String ip;
    private String email = "";
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
        this.ip = plugin.getIP(player);
    }

    private boolean preRegisterCheck() throws Exception {
        String passLow = password.toLowerCase();
        if (PlayerCache.getInstance().isAuthenticated(name)) {
            m.send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
            return false;
        } else if (!Settings.isRegistrationEnabled) {
            m.send(player, MessageKey.REGISTRATION_DISABLED);
            return false;
        } else if (passLow.contains("delete") || passLow.contains("where") || passLow.contains("insert")
            || passLow.contains("modify") || passLow.contains("from") || passLow.contains("select")
            || passLow.contains(";") || passLow.contains("null") || !passLow.matches(Settings.getPassRegex)) {
            // TODO #308: Remove check for SQL keywords
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
            && !ip.equalsIgnoreCase("127.0.0.1")
            && !ip.equalsIgnoreCase("localhost")
            && database.getAllAuthsByIp(ip).size() >= Settings.getmaxRegPerIp) {
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
                emailRegister();
            } else {
                passwordRegister();
            }
        } catch (Exception e) {
            ConsoleLogger.showError(e.getMessage());
            ConsoleLogger.writeStackTrace(e);
            m.send(player, MessageKey.ERROR);
        }
    }

    private void emailRegister() {
        if (Settings.getmaxRegPerEmail > 0
            && !plugin.getPermissionsManager().hasPermission(player, PlayerPermission.ALLOW_MULTIPLE_ACCOUNTS)
            && database.getAllAuthsByEmail(email).size() >= Settings.getmaxRegPerEmail) {
            m.send(player, MessageKey.MAX_REGISTER_EXCEEDED);
            return;
        }
        final EncryptedPassword encryptedPassword = plugin.getPasswordSecurity().computeHash(password, name);
        PlayerAuth auth = PlayerAuth.builder()
            .name(name)
            .realName(player.getName())
            .hash(encryptedPassword)
            .ip(ip)
            .locWorld(player.getLocation().getWorld().getName())
            .locX(player.getLocation().getX())
            .locY(player.getLocation().getY())
            .locZ(player.getLocation().getZ())
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

    private void passwordRegister() throws Exception {
        final EncryptedPassword encryptedPassword = plugin.getPasswordSecurity().computeHash(password, name);
        PlayerAuth auth = PlayerAuth.builder()
            .name(name)
            .realName(player.getName())
            .hash(encryptedPassword)
            .ip(ip)
            .locWorld(player.getLocation().getWorld().getName())
            .locX(player.getLocation().getX())
            .locY(player.getLocation().getY())
            .locZ(player.getLocation().getZ())
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
        ProcessSyncPasswordRegister sync = new ProcessSyncPasswordRegister(player, plugin);
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, sync);
    }
}
