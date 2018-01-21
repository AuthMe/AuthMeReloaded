package fr.xephi.authme.message;

import javax.inject.Inject;

/**
 * File handler for the help_xx.yml resource.
 */
public class HelpMessagesFileHandler extends AbstractMessageFileHandler {

    @Inject // Trigger injection in the superclass
    HelpMessagesFileHandler() {
    }

    @Override
    protected String createFilePath(String language) {
        return "messages/help_" + language + ".yml";
    }

    @Override
    protected String getUpdateCommand() {
        return "/authme messages help";
    }
}
