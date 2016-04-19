package fr.xephi.authme.process.register;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.process.Process;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;
import fr.xephi.authme.util.Utils;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 */
public class ProcessSyncEmailRegister implements Process {

    private final Player player;
    private final String name;
    private final ProcessService service;

    /**
     * Constructor for ProcessSyncEmailRegister.
     *
     * @param player The player to process an email registration for
     * @param service The process service
     */
    public ProcessSyncEmailRegister(Player player, ProcessService service) {
        this.player = player;
        this.name = player.getName().toLowerCase();
        this.service = service;
    }

    @Override
    public void run() {
        LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(name);
        if (!Settings.getRegisteredGroup.isEmpty()) {
            Utils.setGroup(player, Utils.GroupType.REGISTERED);
        }
        service.send(player, MessageKey.ACCOUNT_NOT_ACTIVATED);
        int time = service.getProperty(RestrictionSettings.TIMEOUT) * 20;
        int msgInterval = service.getProperty(RegistrationSettings.MESSAGE_INTERVAL);

        if (limbo != null) {
            if (time != 0) {
                BukkitTask id = service.runTaskLater(new TimeoutTask(service.getAuthMe(), name, player), time);
                limbo.setTimeoutTask(id);
            }
            BukkitTask messageTask = service.runTask(new MessageTask(
                service.getAuthMe(), name, service.retrieveMessage(MessageKey.LOGIN_MESSAGE), msgInterval));
            limbo.setMessageTask(messageTask);
        }

        player.saveData();
        if (!service.getProperty(SecuritySettings.REMOVE_SPAM_FROM_CONSOLE)) {
            ConsoleLogger.info(player.getName() + " registered " + service.getIpAddressManager().getPlayerIp(player));
        }
    }

}
