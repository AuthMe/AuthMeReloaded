package fr.xephi.authme.process;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.process.join.AsyncronousJoin;
import fr.xephi.authme.process.login.AsyncronousLogin;
import fr.xephi.authme.process.logout.AsyncronousLogout;
import fr.xephi.authme.process.quit.AsyncronousQuit;
import fr.xephi.authme.process.register.AsyncronousRegister;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.settings.Settings;

/**
 * 
 * @authors Xephi59,
 *          <a href="http://dev.bukkit.org/profiles/Possible/">Possible</a>
 *
 */
public class Management {

    public AuthMe plugin;
    public static RandomString rdm = new RandomString(Settings.captchaLength);
    public PluginManager pm;

    public Management(AuthMe plugin) {
        this.plugin = plugin;
        this.pm = plugin.getServer().getPluginManager();
    }

    public void performLogin(final Player player, final String password, final boolean forceLogin) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {

            @Override
            public void run() {
                new AsyncronousLogin(player, password, forceLogin, plugin, plugin.database).process();
            }
        });
    }

    public void performRegister(final Player player, final String password, final String email) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {

            @Override
            public void run() {
                new AsyncronousRegister(player, password, email, plugin, plugin.database).process();
            }
        });
    }

    public void performLogout(final Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {

            @Override
            public void run() {
                new AsyncronousLogout(player, plugin, plugin.database).process();
            }
        });
    }

    public void performQuit(final Player player, final boolean isKick) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {

            @Override
            public void run() {
                new AsyncronousQuit(player, plugin, plugin.database, isKick).process();
            }

        });
    }

    public void performJoin(final Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {

            @Override
            public void run() {
                new AsyncronousJoin(player, plugin, plugin.database).process();
            }

        });
    }
}
