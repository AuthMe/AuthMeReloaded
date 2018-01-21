package fr.xephi.authme.message;

import javax.inject.Inject;

/**
 * File handler for the messages_xx.yml resource.
 */
public class MessagesFileHandler extends AbstractMessageFileHandler {

    @Inject // Trigger injection in the superclass
    MessagesFileHandler() {
    }

    @Override
    protected String createFilePath(String language) {
        return "messages/messages_" + language + ".yml";
    }

    @Override
    protected String getUpdateCommand() {
        return "/authme messages";
    }
}
