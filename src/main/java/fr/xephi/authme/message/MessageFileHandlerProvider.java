package fr.xephi.authme.message;

import com.google.common.annotations.VisibleForTesting;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.util.FileUtils;

import javax.inject.Inject;
import java.io.File;
import java.util.function.Function;

/**
 * Injectable creator of {@link MessageFileHandler} instances.
 *
 * @see MessageFileHandler
 */
public class MessageFileHandlerProvider {

    private static final String DEFAULT_LANGUAGE = "en";

    @Inject
    @DataFolder
    private File dataFolder;
    @Inject
    private Settings settings;

    MessageFileHandlerProvider() {
    }

    /**
     * Initializes a message file handler with the messages file of the configured language.
     * Ensures beforehand that the messages file exists or creates it otherwise.
     *
     * @param pathBuilder function taking the configured language code as argument and returning the messages file
     * @return the message file handler
     */
    public MessageFileHandler initializeHandler(Function<String, String> pathBuilder) {
        String language = settings.getProperty(PluginSettings.MESSAGES_LANGUAGE);
        return new MessageFileHandler(
            initializeFile(language, pathBuilder),
            pathBuilder.apply(DEFAULT_LANGUAGE));
    }

    /**
     * Copies the messages file from the JAR if it doesn't exist.
     *
     * @param language the configured language code
     * @param pathBuilder function returning message file name with language as argument
     * @return the messages file to use
     */
    @VisibleForTesting
    File initializeFile(String language, Function<String, String> pathBuilder) {
        String filePath = pathBuilder.apply(language);
        File file = new File(dataFolder, filePath);
        if (FileUtils.copyFileFromResource(file, filePath)) {
            return file;
        }

        String defaultFilePath = pathBuilder.apply(DEFAULT_LANGUAGE);
        if (FileUtils.copyFileFromResource(file, defaultFilePath)) {
            return file;
        }
        return null;
    }

}
