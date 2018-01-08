package fr.xephi.authme.process;

import fr.xephi.authme.process.login.ProcessSyncPlayerLogin;
import fr.xephi.authme.process.logout.ProcessSyncPlayerLogout;
import fr.xephi.authme.process.quit.ProcessSyncPlayerQuit;
import fr.xephi.authme.process.register.ProcessSyncEmailRegister;
import fr.xephi.authme.process.register.ProcessSyncPasswordRegister;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

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
    private ProcessSyncPlayerLogout processSyncPlayerLogout;
    @Inject
    private ProcessSyncPlayerQuit processSyncPlayerQuit;


    public void processSyncEmailRegister(Player player) {
        runTask(() -> processSyncEmailRegister.processEmailRegister(player));
    }

    public void processSyncPasswordRegister(Player player) {
        runTask(() -> processSyncPasswordRegister.processPasswordRegister(player));
    }

    public void processSyncPlayerLogout(Player player) {
        runTask(() -> processSyncPlayerLogout.processSyncLogout(player));
    }

    public void processSyncPlayerLogin(Player player, boolean isFirstLogin, List<String> authsWithSameIp) {
        runTask(() -> processSyncPlayerLogin.processPlayerLogin(player, isFirstLogin, authsWithSameIp));
    }

    public void processSyncPlayerQuit(Player player, boolean wasLoggedIn) {
        runTask(() -> processSyncPlayerQuit.processSyncQuit(player, wasLoggedIn));
    }

    private void runTask(Runnable runnable) {
        bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(runnable);
    }
}
