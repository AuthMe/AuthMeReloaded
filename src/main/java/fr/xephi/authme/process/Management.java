package fr.xephi.authme.process;

import fr.xephi.authme.process.changepassword.AsyncChangePassword;
import fr.xephi.authme.process.email.AsyncAddEmail;
import fr.xephi.authme.process.email.AsyncChangeEmail;
import fr.xephi.authme.process.join.AsynchronousJoin;
import fr.xephi.authme.process.login.AsynchronousLogin;
import fr.xephi.authme.process.logout.AsynchronousLogout;
import fr.xephi.authme.process.quit.AsynchronousQuit;
import fr.xephi.authme.process.register.AsyncRegister;
import fr.xephi.authme.process.register.executors.RegistrationMethod;
import fr.xephi.authme.process.register.executors.RegistrationParameters;
import fr.xephi.authme.process.unregister.AsynchronousUnregister;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

/**
 * Performs auth actions, e.g. when a player joins, registers or wants to change his password.
 */
public class Management {

    @Inject
    private BukkitService bukkitService;

    // Processes
    @Inject
    private AsyncAddEmail asyncAddEmail;
    @Inject
    private AsyncChangeEmail asyncChangeEmail;
    @Inject
    private AsynchronousLogout asynchronousLogout;
    @Inject
    private AsynchronousQuit asynchronousQuit;
    @Inject
    private AsynchronousJoin asynchronousJoin;
    @Inject
    private AsyncRegister asyncRegister;
    @Inject
    private AsynchronousLogin asynchronousLogin;
    @Inject
    private AsynchronousUnregister asynchronousUnregister;
    @Inject
    private AsyncChangePassword asyncChangePassword;

    private KeyedSequentialScheduler<String> asyncUserScheduler;

    Management() {
        this.asyncUserScheduler = new KeyedSequentialScheduler<>(runnable ->
            bukkitService.runTaskAsynchronously(runnable));
    }

    public class KeyedSequentialScheduler<K> {
        private Map<K, SequentialExecutor> executors;
        private Function<Runnable, BukkitTask> scheduler;

        public KeyedSequentialScheduler(Function<Runnable, BukkitTask> scheduler) {
            this.executors = new LinkedHashMap<>();
            this.scheduler = scheduler;
        }

        public void submit(K key, Runnable runnable) {
            SequentialExecutor executor = executors.get(key);
            if (executor == null) {
                executor = new SequentialExecutor(scheduler, () -> executors.remove(key));
                executors.put(key, executor);
            }
            executor.submit(runnable);
        }
    }

    public class SequentialExecutor {
        private Queue<Runnable> queue;
        private Function<Runnable, BukkitTask> scheduler;
        private Runnable callback;

        private BukkitTask executor;

        public SequentialExecutor(Function<Runnable, BukkitTask> scheduler, Runnable callback) {
            this.queue = new LinkedBlockingQueue<>();
            this.scheduler = scheduler;
            this.callback = callback;
        }

        public void submit(Runnable task) {
            queue.add(task);
            if (executor == null || executor.isCancelled()) {
                executor = scheduler.apply(() -> {
                    while (!queue.isEmpty()) {
                        queue.poll().run();
                    }
                    callback.run();
                });
            }
        }
    }

    public void performLogin(Player player, String password) {
        runTask(player, () -> asynchronousLogin.login(player, password));
    }

    public void forceLogin(Player player) {
        runTask(player, () -> asynchronousLogin.forceLogin(player));
    }

    public void performLogout(Player player) {
        runTask(player, () -> asynchronousLogout.logout(player));
    }

    public <P extends RegistrationParameters> void performRegister(RegistrationMethod<P> variant, P parameters) {
        runTask(parameters.getPlayer(), () -> asyncRegister.register(variant, parameters));
    }

    public void performUnregister(Player player, String password) {
        runTask(player, () -> asynchronousUnregister.unregister(player, password));
    }

    public void performUnregisterByAdmin(CommandSender initiator, String name, Player player) {
        runTask(name, () -> asynchronousUnregister.adminUnregister(initiator, name, player));
    }

    public void performJoin(Player player) {
        runTask(player, () -> asynchronousJoin.processJoin(player));
    }

    public void performQuit(Player player) {
        runTask(player, () -> asynchronousQuit.processQuit(player));
    }

    public void performAddEmail(Player player, String newEmail) {
        runTask(player, () -> asyncAddEmail.addEmail(player, newEmail));
    }

    public void performChangeEmail(Player player, String oldEmail, String newEmail) {
        runTask(player, () -> asyncChangeEmail.changeEmail(player, oldEmail, newEmail));
    }

    public void performPasswordChange(Player player, String oldPassword, String newPassword) {
        runTask(player, () -> asyncChangePassword.changePassword(player, oldPassword, newPassword));
    }

    public void performPasswordChangeAsAdmin(CommandSender sender, String playerName, String newPassword) {
        runTask(playerName, () -> asyncChangePassword.changePasswordAsAdmin(sender, playerName, newPassword));
    }

    private void runTask(String playerName, Runnable runnable) {
        if (bukkitService.isUseAsyncTasks()) {
            asyncUserScheduler.submit(playerName.toLowerCase(), runnable);
        } else {
            runnable.run();
        }
    }

    private void runTask(Player player, Runnable runnable) {
        runTask(player.getName(), runnable);
    }
}
