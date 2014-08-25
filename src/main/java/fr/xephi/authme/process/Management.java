package fr.xephi.authme.process;

import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.process.login.AsyncronousLogin;
import fr.xephi.authme.process.register.AsyncronousRegister;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.settings.Settings;

/**
 * 
 * @authors Xephi59, <a
 *          href="http://dev.bukkit.org/profiles/Possible/">Possible</a>
 *
 */
public class Management extends Thread {

    public DataSource database;
    public AuthMe plugin;
    public static RandomString rdm = new RandomString(Settings.captchaLength);
    public PluginManager pm;

    public Management(DataSource database, AuthMe plugin) {
        this.database = database;
        this.plugin = plugin;
        this.pm = plugin.getServer().getPluginManager();
    }

    public void run() {
    }

    public void performLogin(final Player player, final String password,
            final boolean forceLogin) {
        new AsyncronousLogin(player, password, forceLogin, plugin, database).process();
    }

    public void performRegister(final Player player, final String password,
            final String email) {
        new AsyncronousRegister(player, password, email, plugin, database).process();
    }
}
