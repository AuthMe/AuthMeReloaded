package fr.xephi.authme.process;

import fr.xephi.authme.process.login.ProcessSyncPlayerLogin;
import fr.xephi.authme.process.logout.ProcessSynchronousPlayerLogout;
import fr.xephi.authme.process.quit.ProcessSyncronousPlayerQuit;
import fr.xephi.authme.process.register.ProcessSyncEmailRegister;
import fr.xephi.authme.process.register.ProcessSyncPasswordRegister;
import fr.xephi.authme.util.BukkitService;
import org.bukkit.entity.Player;

import javax.inject.Inject;

/**
 * Manager for scheduling synchronous processes internally from the asynchronous processes.
 * These synchronous processes are a continuation of the associated async processes; they only
 * contain certain tasks which may only be run synchronously (most interactions with Bukkit).
 * These synchronous tasks should never be called aside from the asynchronous processes.
 *
 * @see Management
 */
public class SyncProcessManager {

    @Inject
    private BukkitService bukkitService;

    @Inject
    private ProcessSyncEmailRegister processSyncEmailRegister;
    @Inject
    private ProcessSyncPasswordRegister processSyncPasswordRegister;
    @Inject
    private ProcessSyncPlayerLogin processSyncPlayerLogin;
    @Inject
    private ProcessSynchronousPlayerLogout processSynchronousPlayerLogout;
    @Inject
    private ProcessSyncronousPlayerQuit processSyncronousPlayerQuit;


    public void processSyncEmailRegister(final Player player) {
        runTask(new Runnable() {
            @Override
            public void run() {
                processSyncEmailRegister.processEmailRegister(player);
            }
        });
    }

    public void processSyncPasswordRegister(final Player player) {
        runTask(new Runnable() {
            @Override
            public void run() {
                processSyncPasswordRegister.processPasswordRegister(player);
            }
        });
    }

    public void processSyncPlayerLogout(final Player player) {
        runTask(new Runnable() {
            @Override
            public void run() {
                processSynchronousPlayerLogout.processSyncLogout(player);
            }
        });
    }

    public void processSyncPlayerLogin(final Player player) {
        runTask(new Runnable() {
            @Override
            public void run() {
                processSyncPlayerLogin.processPlayerLogin(player);
            }
        });
    }

    public void processSyncPlayerQuit(final Player player, final boolean isOp, final boolean needToChange) {
        runTask(new Runnable() {
            @Override
            public void run() {
                processSyncronousPlayerQuit.processSyncQuit(player, isOp, needToChange);
            }
        });
    }

    private void runTask(Runnable runnable) {
        bukkitService.scheduleSyncDelayedTask(runnable);
    }
}
