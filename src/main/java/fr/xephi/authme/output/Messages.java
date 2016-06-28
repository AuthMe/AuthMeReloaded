package fr.xephi.authme.output;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.inject.Inject;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Class for retrieving and sending translatable messages to players.
 */
public class Messages implements SettingsDependent {

    private FileConfiguration configuration;
    private String fileName;
    private final String defaultFile;
    private FileConfiguration defaultConfiguration;

    /**
     * Constructor.
     *
     * @param settings The settings
     */
    @Inject
    Messages(NewSetting settings) {
        loadSettings(settings);
        this.defaultFile = settings.getDefaultMessagesFile();
    }

    /**
     * Send the given message code to the player.
     *
     * @param sender The entity to send the message to
     * @param key The key of the message to send
     */
    public void send(CommandSender sender, MessageKey key) {
        String[] lines = retrieve(key);
        for (String line : lines) {
            sender.sendMessage(line);
        }
    }

    /**
     * Send the given message code to the player with the given tag replacements. Note that this method
     * logs an error if the number of supplied replacements doesn't correspond to the number of tags
     * the message key contains.
     *
     * @param sender The entity to send the message to
     * @param key The key of the message to send
     * @param replacements The replacements to apply for the tags
     */
    public void send(CommandSender sender, MessageKey key, String... replacements) {
        String message = retrieveSingle(key, replacements);
        for (String line : message.split("\n")) {
            sender.sendMessage(line);
        }
    }

    /**
     * Retrieve the message from the text file and return it split by new line as an array.
     *
     * @param key The message key to retrieve
     * @return The message split by new lines
     */
    public String[] retrieve(MessageKey key) {
        final String code = key.getKey();
        String message = configuration.getString(code);

        if (message == null) {
            ConsoleLogger.showError("Error getting message with key '" + code + "'. "
                + "Please verify your config file at '" + fileName + "'");
            return formatMessage(getDefault(code));
        }
        if(message.isEmpty()) {
            return new String[0];
        }
        return formatMessage(message);
    }

    /**
     * Retrieve the message from the text file.
     *
     * @param key The message key to retrieve
     * @return The message from the file
     */
    public String retrieveSingle(MessageKey key) {
        return StringUtils.join("\n", retrieve(key));
    }

    /**
     * Retrieve the given message code with the given tag replacements. Note that this method
     * logs an error if the number of supplied replacements doesn't correspond to the number of tags
     * the message key contains.
     *
     * @param key The key of the message to send
     * @param replacements The replacements to apply for the tags
     * @return The message from the file with replacements
     */
    public String retrieveSingle(MessageKey key, String... replacements) {
        String message = retrieveSingle(key);
        String[] tags = key.getTags();
        if (replacements.length == tags.length) {
            for (int i = 0; i < tags.length; ++i) {
                message = message.replace(tags[i], replacements[i]);
            }
        } else {
            ConsoleLogger.showError("Invalid number of replacements for message key '" + key + "'");
        }
        return message;
    }

    @Override
    public void loadSettings(NewSetting settings) {
        File messageFile = settings.getMessagesFile();
        this.configuration = YamlConfiguration.loadConfiguration(messageFile);
        this.fileName = messageFile.getName();
    }

    private String getDefault(String code) {
        if (defaultFile == null) {
            return getDefaultErrorMessage(code);
        }

        if (defaultConfiguration == null) {
            InputStream stream = Messages.class.getResourceAsStream(defaultFile);
            defaultConfiguration = YamlConfiguration.loadConfiguration(new InputStreamReader(stream));
        }
        String message = defaultConfiguration.getString(code);
        return message == null ? getDefaultErrorMessage(code) : message;
    }

    private static String getDefaultErrorMessage(String code) {
        return "Error retrieving message '" + code + "'";
    }

    private static String[] formatMessage(String message) {
        String[] lines = message.split("&n");
        for (int i = 0; i < lines.length; ++i) {
            lines[i] = ChatColor.translateAlternateColorCodes('&', lines[i]);
        }
        return lines;
    }

}
