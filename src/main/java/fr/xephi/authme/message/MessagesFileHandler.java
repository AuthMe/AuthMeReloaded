package fr.xephi.authme.message;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.message.updater.MessageUpdater;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.settings.Settings;

import javax.inject.Inject;
import java.io.File;

import static fr.xephi.authme.message.MessagePathHelper.DEFAULT_LANGUAGE;

/**
 * File handler for the messages_xx.yml resource.
 */
public class MessagesFileHandler extends AbstractMessageFileHandler {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(MessagesFileHandler.class);

    private final MessageUpdater messageUpdater;

    @Inject
    MessagesFileHandler(@DataFolder File dataFolder, Settings settings, MessageUpdater messageUpdater) {
        super(dataFolder, settings);
        this.messageUpdater = messageUpdater;
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
                logger.warning("Migration after reload attempt");
            } else {
                reloadInternal(true);
            }
        }
    }

    @Override
    protected String createFilePath(String language) {
        return MessagePathHelper.createMessageFilePath(language);
    }
}
