package fr.xephi.authme.message;

import com.google.common.collect.ImmutableMap;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.util.expiring.Duration;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Class for retrieving and sending translatable messages to players.
 */
public class Messages {

    // Custom Authme tag replaced to new line
    private static final String NEWLINE_TAG = "%nl%";

    private static final String PLAYER_TAG = "%username%";

    /** Contains the keys of the singular messages for time units. */
    private static final Map<TimeUnit, MessageKey> TIME_UNIT_SINGULARS = ImmutableMap.<TimeUnit, MessageKey>builder()
        .put(TimeUnit.SECONDS, MessageKey.SECOND)
        .put(TimeUnit.MINUTES, MessageKey.MINUTE)
        .put(TimeUnit.HOURS, MessageKey.HOUR)
        .put(TimeUnit.DAYS, MessageKey.DAY).build();

    /** Contains the keys of the plural messages for time units. */
    private static final Map<TimeUnit, MessageKey> TIME_UNIT_PLURALS = ImmutableMap.<TimeUnit, MessageKey>builder()
        .put(TimeUnit.SECONDS, MessageKey.SECONDS)
        .put(TimeUnit.MINUTES, MessageKey.MINUTES)
        .put(TimeUnit.HOURS, MessageKey.HOURS)
        .put(TimeUnit.DAYS, MessageKey.DAYS).build();

    private MessagesFileHandler messagesFileHandler;

    /*
     * Constructor.
     */
    @Inject
    Messages(MessagesFileHandler messagesFileHandler) {
        this.messagesFileHandler = messagesFileHandler;
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
            sender.sendMessage(line.replace(PLAYER_TAG, sender.getName()));
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
        String message = retrieveSingle(key, replacements).replace(PLAYER_TAG, sender.getName());
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
        String message = retrieveMessage(key);
        if (message.isEmpty()) {
            // Return empty array instead of array with 1 empty string as entry
            return new String[0];
        }
        return message.split("\n");
    }

    /**
     * Returns the textual representation for the given duration.
     * Note that this class only supports the time units days, hour, minutes and seconds.
     *
     * @param duration the duration to build a text of
     * @return text of the duration
     */
    public String formatDuration(Duration duration) {
        long value = duration.getDuration();
        MessageKey timeUnitKey = value == 1
            ? TIME_UNIT_SINGULARS.get(duration.getTimeUnit())
            : TIME_UNIT_PLURALS.get(duration.getTimeUnit());

        return value + " " + retrieveMessage(timeUnitKey);
    }

    /**
     * Retrieve the message from the text file.
     *
     * @param key The message key to retrieve
     * @return The message from the file
     */
    private String retrieveMessage(MessageKey key) {
        return formatMessage(
            messagesFileHandler.getMessage(key.getKey()));
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
        String message = retrieveMessage(key);
        String[] tags = key.getTags();
        if (replacements.length == tags.length) {
            for (int i = 0; i < tags.length; ++i) {
                message = message.replace(tags[i], replacements[i]);
            }
        } else {
            ConsoleLogger.warning("Invalid number of replacements for message key '" + key + "'");
        }
        return message;
    }

    private static String formatMessage(String message) {
        return ChatColor.translateAlternateColorCodes('&', message)
            .replace(NEWLINE_TAG, "\n");
    }

}
