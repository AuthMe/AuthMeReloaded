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
 * @author Gabriele
 * @version $Revision: 1.0 $
 */
public class Management {

    public static RandomString rdm = new RandomString(Settings.captchaLength);
    private final AuthMe plugin;
    private final BukkitScheduler sched;

    /**
     * Constructor for Management.
     *
     * @param plugin AuthMe
     */
    public Management(AuthMe plugin) {
        this.plugin = plugin;
        this.sched = this.plugin.getServer().getScheduler();
    }

    /**
     * Method performLogin.
     *
     * @param player     Player
     * @param password   String
     * @param forceLogin boolean
     */
    public void performLogin(final Player player, final String password, final boolean forceLogin) {
        sched.runTaskAsynchronously(plugin, new Runnable() {

            @Override
            public void run() {
                new AsyncronousLogin(player, password, forceLogin, plugin, plugin.database).process();
            }
        });
    }

    /**
     * Method performLogout.
     *
     * @param player Player
     */
    public void performLogout(final Player player) {
        sched.runTaskAsynchronously(plugin, new Runnable() {

            @Override
            public void run() {
                new AsyncronousLogout(player, plugin, plugin.database).process();
            }
        });
    }

    /**
     * Method performRegister.
     *
     * @param player   Player
     * @param password String
     * @param email    String
     */
    public void performRegister(final Player player, final String password, final String email) {
        sched.runTaskAsynchronously(plugin, new Runnable() {

            @Override
            public void run() {
                new AsyncRegister(player, password, email, plugin, plugin.database).process();
            }
        });
    }

    /**
     * Method performUnregister.
     *
     * @param player   Player
     * @param password String
     * @param force    boolean
     */
    public void performUnregister(final Player player, final String password, final boolean force) {
        sched.runTaskAsynchronously(plugin, new Runnable() {

            @Override
            public void run() {
                new AsyncronousUnregister(player, password, force, plugin).process();
            }
        });
    }

    /**
     * Method performJoin.
     *
     * @param player Player
     */
    public void performJoin(final Player player) {
        sched.runTaskAsynchronously(plugin, new Runnable() {

            @Override
            public void run() {
                new AsyncronousJoin(player, plugin, plugin.database).process();
            }

        });
    }

    /**
     * Method performQuit.
     *
     * @param player Player
     * @param isKick boolean
     */
    public void performQuit(final Player player, final boolean isKick) {
        sched.runTaskAsynchronously(plugin, new Runnable() {

            @Override
            public void run() {
                new AsyncronousQuit(player, plugin, plugin.database, isKick).process();
            }

        });
    }

    /**
     * Method performAddEmail.
     *
     * @param player         Player
     * @param newEmail       String
     * @param newEmailVerify String
     */
    public void performAddEmail(final Player player, final String newEmail, final String newEmailVerify) {
        sched.runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                new AsyncChangeEmail(player, plugin, null, newEmail, newEmailVerify).process();
            }
        });
    }

    /**
     * Method performChangeEmail.
     *
     * @param player   Player
     * @param oldEmail String
     * @param newEmail String
     */
    public void performChangeEmail(final Player player, final String oldEmail, final String newEmail) {
        sched.runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                new AsyncChangeEmail(player, plugin, oldEmail, newEmail).process();
            }
        });
    }
}
