package fr.xephi.authme.message;

import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.util.FileUtils;

import javax.inject.Inject;
import java.io.File;
import java.util.function.Function;

public class MessageFileCopier {

    private static final String DEFAULT_LANGUAGE = "en";

    private final File dataFolder;
    private final Settings settings;

    @Inject
    MessageFileCopier(@DataFolder File dataFolder, Settings settings) {
        this.dataFolder = dataFolder;
        this.settings = settings;
    }

    public MessageFileData initializeData(Function<String, String> pathBuilder) {
        String language = settings.getProperty(PluginSettings.MESSAGES_LANGUAGE);
        return new MessageFileData(
            initializeFile(language, pathBuilder),
            pathBuilder.apply(DEFAULT_LANGUAGE));
    }

    private File initializeFile(String language, Function<String, String> pathBuilder) {
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

    public static final class MessageFileData {
        private final File file;
        private final String defaultFile;

        MessageFileData(File file, String defaultFile) {
            this.file = file;
            this.defaultFile = defaultFile;
        }

        public File getFile() {
            return file;
        }

        public String getDefaultFile() {
            return defaultFile;
        }
    }
}
