package fr.xephi.authme.process;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.process.email.AsyncChangeEmail;
import fr.xephi.authme.process.join.AsyncronousJoin;
import fr.xephi.authme.process.login.AsyncronousLogin;
import fr.xephi.authme.process.logout.AsyncronousLogout;
import fr.xephi.authme.process.quit.AsyncronousQuit;
import fr.xephi.authme.process.register.AsyncRegister;
import fr.xephi.authme.process.unregister.AsyncronousUnregister;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.settings.Settings;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

/**
 * @authors Xephi59,
 * <a href="http://dev.bukkit.org/profiles/Possible/">Possible</a>
 */
public class Management {

    private final AuthMe pl;
    private final BukkitScheduler sched;
    public static RandomString rdm = new RandomString(Settings.captchaLength);

    public Management(AuthMe plugin) {
        this.pl = plugin;
        this.sched = pl.getServer().getScheduler();
    }

    public void performLogin(final Player player, final String password, final boolean forceLogin) {
        sched.runTaskAsynchronously(pl, new Runnable() {

            @Override
            public void run() {
                new AsyncronousLogin(player, password, forceLogin, pl, pl.database).process();
            }
        });
    }

    public void performLogout(final Player player) {
        sched.runTaskAsynchronously(pl, new Runnable() {

            @Override
            public void run() {
                new AsyncronousLogout(player, pl, pl.database).process();
            }
        });
    }

    public void performRegister(final Player player, final String password, final String email) {
        sched.runTaskAsynchronously(pl, new Runnable() {

            @Override
            public void run() {
                new AsyncRegister(player, password, email, pl, pl.database).process();
            }
        });
    }

    public void performUnregister(final Player player, final String password, final boolean force) {
        sched.runTaskAsynchronously(pl, new Runnable() {

            @Override
            public void run() {
                new AsyncronousUnregister(player, password, force, pl).process();
            }
        });
    }

    public void performJoin(final Player player) {
        sched.runTaskAsynchronously(pl, new Runnable() {

            @Override
            public void run() {
                new AsyncronousJoin(player, pl, pl.database).process();
            }

        });
    }

    public void performQuit(final Player player, final boolean isKick) {
        sched.runTaskAsynchronously(pl, new Runnable() {

            @Override
            public void run() {
                new AsyncronousQuit(player, pl, pl.database, isKick).process();
            }

        });
    }

}
