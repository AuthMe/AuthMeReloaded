package fr.xephi.authme.command.executable.authme;

import ch.jalu.datasourcecolumns.data.DataSourceValue;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.service.CommonService;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.List;

/**
 * Returns a player's email.
 */
public class GetEmailCommand implements ExecutableCommand {

    @Inject
    private DataSource dataSource;

    @Inject
    private CommonService commonService;

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments) {
        String playerName = arguments.isEmpty() ? sender.getName() : arguments.get(0);

        DataSourceValue<String> email = dataSource.getEmail(playerName);
        if (email.rowExists()) {
            sender.sendMessage("[AuthMe] " + playerName + "'s email: " + email.getValue());
        } else {
            commonService.send(sender, MessageKey.UNKNOWN_USER);
        }
    }
}
