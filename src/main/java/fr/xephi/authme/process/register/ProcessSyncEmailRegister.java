package fr.xephi.authme.process.register;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.permission.AuthGroupType;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.process.SynchronousProcess;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.Utils;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import javax.inject.Inject;

import static fr.xephi.authme.util.BukkitService.TICKS_PER_SECOND;


public class ProcessSyncEmailRegister implements SynchronousProcess {

    @Inject
    private ProcessService service;

    @Inject
    private LimboCache limboCache;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private AuthMe authMe;

    ProcessSyncEmailRegister() { }

    public void processEmailRegister(Player player) {
        final String name = player.getName().toLowerCase();
        LimboPlayer limbo = limboCache.getLimboPlayer(name);
        if (!Settings.getRegisteredGroup.isEmpty()) {
            service.setGroup(player, AuthGroupType.REGISTERED);
        }
        service.send(player, MessageKey.ACCOUNT_NOT_ACTIVATED);
        int time = service.getProperty(RestrictionSettings.TIMEOUT) * TICKS_PER_SECOND;
        int msgInterval = service.getProperty(RegistrationSettings.MESSAGE_INTERVAL);

        if (limbo != null) {
            if (time != 0) {
                BukkitTask id = bukkitService.runTaskLater(new TimeoutTask(authMe, name, player), time);
                limbo.setTimeoutTask(id);
            }
            BukkitTask messageTask = bukkitService.runTask(new MessageTask(
                bukkitService, name, service.retrieveMessage(MessageKey.LOGIN_MESSAGE), msgInterval));
            limbo.setMessageTask(messageTask);
        }

        player.saveData();
        if (!service.getProperty(SecuritySettings.REMOVE_SPAM_FROM_CONSOLE)) {
            ConsoleLogger.info(player.getName() + " registered " + Utils.getPlayerIp(player));
        }
    }

}
