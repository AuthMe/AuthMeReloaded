package fr.xephi.authme.process.register;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.permission.AuthGroupType;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.process.SynchronousProcess;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.task.LimboPlayerTaskManager;
import fr.xephi.authme.util.PlayerUtils;
import org.bukkit.entity.Player;

import javax.inject.Inject;


public class ProcessSyncEmailRegister implements SynchronousProcess {

    @Inject
    private ProcessService service;

    @Inject
    private LimboPlayerTaskManager limboPlayerTaskManager;

    ProcessSyncEmailRegister() { }


    public void processEmailRegister(Player player) {
        final String name = player.getName().toLowerCase();
        if (!service.getProperty(HooksSettings.REGISTERED_GROUP).isEmpty()) {
            service.setGroup(player, AuthGroupType.REGISTERED);
        }
        service.send(player, MessageKey.ACCOUNT_NOT_ACTIVATED);

        limboPlayerTaskManager.registerTimeoutTask(player);
        limboPlayerTaskManager.registerMessageTask(name, true);

        player.saveData();
        ConsoleLogger.fine(player.getName() + " registered " + PlayerUtils.getPlayerIp(player));
    }

}
