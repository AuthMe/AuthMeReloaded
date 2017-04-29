package fr.xephi.authme.process.register;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.SynchronousProcess;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.util.PlayerUtils;
import org.bukkit.entity.Player;

import javax.inject.Inject;


public class ProcessSyncEmailRegister implements SynchronousProcess {

    @Inject
    private CommonService service;

    @Inject
    private LimboService limboService;

    ProcessSyncEmailRegister() {
    }

    public void processEmailRegister(Player player) {
        service.send(player, MessageKey.ACCOUNT_NOT_ACTIVATED);
        limboService.replaceTasksAfterRegistration(player);

        player.saveData();
        ConsoleLogger.fine(player.getName() + " registered " + PlayerUtils.getPlayerIp(player));
    }

}
