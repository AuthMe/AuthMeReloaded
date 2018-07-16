package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.command.help.HelpMessagesService;
import fr.xephi.authme.service.HelpTranslationGenerator;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Messages command, updates the user's help messages file with any missing files
 * from the provided file in the JAR.
 */
public class UpdateHelpMessagesCommand implements ExecutableCommand {

    @Inject
    private HelpTranslationGenerator helpTranslationGenerator;
    @Inject
    private HelpMessagesService helpMessagesService;

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments) {
        try {
            File updatedFile = helpTranslationGenerator.updateHelpFile();
            sender.sendMessage("Successfully updated the help file '" + updatedFile.getName() + "'");
            helpMessagesService.reloadMessagesFile();
        } catch (IOException e) {
            sender.sendMessage("Could not update help file: " + e.getMessage());
            ConsoleLogger.logException("Could not update help file:", e);
        }
    }
}
