package fr.xephi.authme.process.register;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.permission.AuthGroupType;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.process.SynchronousProcess;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.task.LimboPlayerTaskManager;
import fr.xephi.authme.util.Utils;
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
        if (!Settings.getRegisteredGroup.isEmpty()) {
            service.setGroup(player, AuthGroupType.REGISTERED);
        }
        service.send(player, MessageKey.ACCOUNT_NOT_ACTIVATED);

        limboPlayerTaskManager.registerTimeoutTask(player);
        limboPlayerTaskManager.registerMessageTask(name, true);

        player.saveData();
        if (!service.getProperty(SecuritySettings.REMOVE_SPAM_FROM_CONSOLE)) {
            ConsoleLogger.info(player.getName() + " registered " + Utils.getPlayerIp(player));
        }
    }

}
