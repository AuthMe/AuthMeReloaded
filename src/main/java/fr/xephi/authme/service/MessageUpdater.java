package fr.xephi.authme.service;

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

/**
 * Updates a user's messages file with messages from the JAR files.
 */
public class MessageUpdater {

    private final File userFile;
    private final FileConfiguration userConfiguration;
    private final FileConfiguration localJarConfiguration;
    private final FileConfiguration defaultJarConfiguration;
    private boolean hasMissingMessages = false;

    public MessageUpdater(File userFile, String jarFile, String jarDefaultsFile) throws Exception {
        if (!userFile.exists()) {
            throw new Exception("Local messages file does not exist");
        }
        this.userFile = userFile;
        this.userConfiguration = YamlConfiguration.loadConfiguration(userFile);

        localJarConfiguration = loadJarFileOrSendError(jarFile);
        defaultJarConfiguration = jarFile.equals(jarDefaultsFile)
            ? null
            : loadJarFileOrSendError(jarDefaultsFile);
        if (localJarConfiguration == null && defaultJarConfiguration == null) {
            throw new Exception("Could not load any JAR messages file to copy from");
        }
    }

    public void executeCopy(CommandSender sender) {
        copyMissingMessages();

        if (!hasMissingMessages) {
            sender.sendMessage("No new messages to add");
            return;
        }

        // Save user configuration file
        try {
            userConfiguration.save(userFile);
            sender.sendMessage("Message file updated with new messages");
        } catch (IOException e) {
            sender.sendMessage("Could not save to messages file");
            ConsoleLogger.logException("Could not save new messages to file:", e);
        }
    }

    private void copyMissingMessages() {
        for (MessageKey entry : MessageKey.values()) {
            final String key = entry.getKey();
            if (!userConfiguration.contains(key)) {
                String jarMessage = getMessageFromJar(key);
                if (jarMessage != null) {
                    hasMissingMessages = true;
                    userConfiguration.set(key, jarMessage);
                }
            }
        }
    }

    private String getMessageFromJar(String key) {
        String message = (localJarConfiguration == null ? null : localJarConfiguration.getString(key));
        if (message != null) {
            return message;
        }
        return (defaultJarConfiguration == null ? null : defaultJarConfiguration.getString(key));
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
