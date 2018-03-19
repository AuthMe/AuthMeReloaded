package fr.xephi.authme.message;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.message.updater.MessageUpdater;

import javax.inject.Inject;

/**
 * File handler for the messages_xx.yml resource.
 */
public class MessagesFileHandler extends AbstractMessageFileHandler {

    @Inject
    private MessageUpdater messageUpdater;

    MessagesFileHandler() {
    }

    @Override
    public void reload() {
        reloadInternal(false);
    }

    private void reloadInternal(boolean isFromReload) {
        super.reload();

        String language = getLanguage();
        boolean hasChange = messageUpdater.migrateAndSave(
            getUserLanguageFile(), createFilePath(language), createFilePath(DEFAULT_LANGUAGE));
        if (hasChange) {
            if (isFromReload) {
                ConsoleLogger.warning("Migration after reload attempt");
            } else {
                reloadInternal(true);
            }
        }
    }

    @Override
    protected String createFilePath(String language) {
        return "messages/messages_" + language + ".yml";
    }
}
