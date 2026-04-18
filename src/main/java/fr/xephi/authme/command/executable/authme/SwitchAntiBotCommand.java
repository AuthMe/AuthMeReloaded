package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.CommandMapper;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.command.FoundCommandResult;
import fr.xephi.authme.command.help.HelpProvider;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.service.AntiBotService;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

/**
 * Display or change the status of the antibot mod.
 */
public class SwitchAntiBotCommand implements ExecutableCommand {

    @Inject
    private AntiBotService antiBotService;

    @Inject
    private CommandMapper commandMapper;

    @Inject
    private HelpProvider helpProvider;

    @Inject
    private Messages messages;

    @Override
    public void executeCommand(final CommandSender sender, List<String> arguments) {
        if (arguments.isEmpty()) {
            messages.send(sender, MessageKey.ANTIBOT_STATUS, antiBotService.getAntiBotStatus().name());
            return;
        }

        String newState = arguments.get(0);

        if ("ON".equalsIgnoreCase(newState)) {
            antiBotService.overrideAntiBotStatus(true);
            messages.send(sender, MessageKey.ANTIBOT_OVERRIDE_ENABLED);
        } else if ("OFF".equalsIgnoreCase(newState)) {
            antiBotService.overrideAntiBotStatus(false);
            messages.send(sender, MessageKey.ANTIBOT_OVERRIDE_DISABLED);
        } else {
            messages.send(sender, MessageKey.ANTIBOT_INVALID_MODE);
            FoundCommandResult result = commandMapper.mapPartsToCommand(sender, Arrays.asList("authme", "antibot"));
            helpProvider.outputHelp(sender, result, HelpProvider.SHOW_ARGUMENTS);
            messages.send(sender, MessageKey.COMMAND_DETAILED_HELP, "authme help antibot");
        }
    }
}
