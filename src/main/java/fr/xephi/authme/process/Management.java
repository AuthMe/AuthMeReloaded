package fr.xephi.authme.process;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.process.email.AsyncAddEmail;
import fr.xephi.authme.process.email.AsyncChangeEmail;
import fr.xephi.authme.process.join.AsynchronousJoin;
import fr.xephi.authme.process.login.AsynchronousLogin;
import fr.xephi.authme.process.logout.AsynchronousLogout;
import fr.xephi.authme.process.quit.AsynchronousQuit;
import fr.xephi.authme.process.register.AsyncRegister;
import fr.xephi.authme.process.unregister.AsynchronousUnregister;
import fr.xephi.authme.settings.NewSetting;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

/**
 */
public class Management {

    private final AuthMe plugin;
    private final BukkitScheduler sched;
    private final ProcessService processService;
    private final DataSource dataSource;
    private final PlayerCache playerCache;
    private final NewSetting settings;

    /**
     * Constructor for Management.
     *
     * @param plugin AuthMe
     */
    public Management(AuthMe plugin, ProcessService processService, DataSource dataSource, PlayerCache playerCache) {
        this.plugin = plugin;
        this.sched = this.plugin.getServer().getScheduler();
        this.processService = processService;
        this.dataSource = dataSource;
        this.playerCache = playerCache;

        // FIXME don't pass settings anymore -> go through the service in the processes
        this.settings = processService.getSettings();
    }

    public void performLogin(final Player player, final String password, final boolean forceLogin) {
        sched.runTaskAsynchronously(plugin, new Runnable() {

            @Override
            public void run() {
                new AsynchronousLogin(player, password, forceLogin, plugin, dataSource, settings)
                    .process();
            }
        });
    }

    public void performLogout(final Player player) {
        sched.runTaskAsynchronously(plugin, new Runnable() {

            @Override
            public void run() {
                new AsynchronousLogout(player, plugin, plugin.getDataSource()).process();
            }
        });
    }

    public void performRegister(final Player player, final String password, final String email) {
        sched.runTaskAsynchronously(plugin, new Runnable() {

            @Override
            public void run() {
                new AsyncRegister(player, password, email, plugin, dataSource, settings).process();
            }
        });
    }

    public void performUnregister(final Player player, final String password, final boolean force) {
        sched.runTaskAsynchronously(plugin, new Runnable() {

            @Override
            public void run() {
                new AsynchronousUnregister(player, password, force, plugin).process();
            }
        });
    }

    public void performJoin(final Player player) {
        sched.runTaskAsynchronously(plugin, new Runnable() {

            @Override
            public void run() {
                new AsynchronousJoin(player, plugin, dataSource).process();
            }

        });
    }

    public void performQuit(final Player player, final boolean isKick) {
        sched.runTaskAsynchronously(plugin, new Runnable() {

            @Override
            public void run() {
                new AsynchronousQuit(player, plugin, dataSource, isKick).process();
            }

        });
    }

    public void performAddEmail(final Player player, final String newEmail) {
        runTask(new AsyncAddEmail(player, newEmail, dataSource, playerCache, processService));
    }

    public void performChangeEmail(final Player player, final String oldEmail, final String newEmail) {
        sched.runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                new AsyncChangeEmail(player, plugin, oldEmail, newEmail, dataSource, playerCache, settings).process();
            }
        });
    }

    private void runTask(Process process) {
        sched.runTaskAsynchronously(plugin, process);
    }
}
