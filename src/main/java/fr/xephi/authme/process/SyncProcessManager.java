package fr.xephi.authme.process;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.process.login.ProcessSyncPlayerLogin;
import fr.xephi.authme.process.logout.ProcessSyncPlayerLogout;
import fr.xephi.authme.process.quit.ProcessSyncPlayerQuit;
import fr.xephi.authme.process.register.ProcessSyncEmailRegister;
import fr.xephi.authme.process.register.ProcessSyncPasswordRegister;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.entity.Entity;
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


    private final ConsoleLogger logger = ConsoleLoggerFactory.get(SyncProcessManager.class);

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
        runTask("EmailRegister", player, () -> processSyncEmailRegister.processEmailRegister(player));
    }

    public void processSyncPasswordRegister(Player player) {
        runTask("PasswordRegister", player, () -> processSyncPasswordRegister.processPasswordRegister(player));
    }

    public void processSyncPlayerLogout(Player player) {
        runTask("PlayerLogout", player, () -> processSyncPlayerLogout.processSyncLogout(player));
    }

    public void processSyncPlayerLogin(Player player, boolean isFirstLogin, List<String> authsWithSameIp) {
        runTask("PlayerLogin", player, () -> processSyncPlayerLogin.processPlayerLogin(player, isFirstLogin, authsWithSameIp));
    }

    public void processSyncPlayerQuit(Player player, boolean wasLoggedIn) {
        runTask("PlayerQuit", player, () -> processSyncPlayerQuit.processSyncQuit(player, wasLoggedIn));
    }

    private void runTask(String taskName, Entity entity, Runnable runnable) {
        bukkitService.executeOptionallyOnEntityScheduler(entity, runnable, () -> {
            String entityName;
            try {
                entityName = entity.getName();
            } catch (Exception ex) {
                entityName = "<none>";
            }
            // todo: should the tasks be executed anyway or not? I left this warning message to remind about this doubt.
            logger.warning("Task " + taskName + " has not been executed because the entity " + entityName + " is not available anymore.");
        });
    }
}
