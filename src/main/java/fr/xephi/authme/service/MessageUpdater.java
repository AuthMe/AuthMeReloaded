package fr.xephi.authme.service;

import com.github.authme.configme.SettingsManager;
import com.github.authme.configme.knownproperties.ConfigurationData;
import com.github.authme.configme.properties.Property;
import com.github.authme.configme.properties.StringProperty;
import com.github.authme.configme.resource.YamlFileResource;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.util.FileUtils;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Updates a user's messages file with messages from the JAR files.
 */
public class MessageUpdater {

    private final FileConfiguration userConfiguration;
    private final FileConfiguration localJarConfiguration;
    private final FileConfiguration defaultJarConfiguration;

    private final List<Property<String>> properties;
    private final SettingsManager settingsManager;
    private boolean hasMissingMessages = false;

    /**
     * Constructor.
     *
     * @param userFile       messages file in the data folder
     * @param localJarFile   path to messages file in JAR in local language
     * @param defaultJarFile path to messages file in JAR for default language
     * @throws Exception     if userFile does not exist or no JAR messages file can be loaded
     */
    public MessageUpdater(File userFile, String localJarFile, String defaultJarFile) throws Exception {
        if (!userFile.exists()) {
            throw new Exception("Local messages file does not exist");
        }

        userConfiguration = YamlConfiguration.loadConfiguration(userFile);
        localJarConfiguration = loadJarFileOrSendError(localJarFile);
        defaultJarConfiguration = localJarFile.equals(defaultJarFile) ? null : loadJarFileOrSendError(defaultJarFile);
        if (localJarConfiguration == null && defaultJarConfiguration == null) {
            throw new Exception("Could not load any JAR messages file to copy from");
        }

        properties = buildPropertyEntriesForMessageKeys();
        settingsManager = new SettingsManager(
            new YamlFileResource(userFile), null, new ConfigurationData(properties));
    }

    /**
     * Copies missing messages to the messages file.
     *
     * @param sender sender starting the copy process
     * @return true if the messages file was updated, false otherwise
     * @throws Exception if an error occurs during saving
     */
    public boolean executeCopy(CommandSender sender) throws Exception {
        copyMissingMessages();

        if (!hasMissingMessages) {
            sender.sendMessage("No new messages to add");
            return false;
        }

        // Save user configuration file
        try {
            settingsManager.save();
            sender.sendMessage("Message file updated with new messages");
            return true;
        } catch (Exception e) {
            throw new Exception("Could not save to messages file: " + StringUtils.formatException(e));
        }
    }

    @SuppressWarnings("unchecked")
    private void copyMissingMessages() {
        for (Property<String> property : properties) {
            String message = userConfiguration.getString(property.getPath());
            if (message == null) {
                hasMissingMessages = true;
                message = getMessageFromJar(property.getPath());
            }
            settingsManager.setProperty(property, message);
        }
    }

    private String getMessageFromJar(String key) {
        String message = (localJarConfiguration == null ? null : localJarConfiguration.getString(key));
        if (message != null) {
            return message;
        }
        return (defaultJarConfiguration == null) ? null : defaultJarConfiguration.getString(key);
    }

    private static FileConfiguration loadJarFileOrSendError(String jarPath) {
        try (InputStream stream = FileUtils.getResourceFromJar(jarPath)) {
            if (stream == null) {
                ConsoleLogger.info("Could not load '" + jarPath + "' from JAR");
                return null;
            }
            InputStreamReader isr = new InputStreamReader(stream);
            FileConfiguration configuration = YamlConfiguration.loadConfiguration(isr);
            close(isr);
            return configuration;
        } catch (IOException e) {
            ConsoleLogger.logException("Exception while handling JAR path '" + jarPath + "'", e);
        }
        return null;
    }

    private static List<Property<String>> buildPropertyEntriesForMessageKeys() {
        return Arrays.stream(MessageKey.values())
            .map(key -> new StringProperty(key.getKey(), ""))
            .collect(Collectors.toList());
    }

    private static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                ConsoleLogger.info("Cannot close '" + closeable + "': " + StringUtils.formatException(e));
            }
        }
    }
}
