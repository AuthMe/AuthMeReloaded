package messages;

import fr.xephi.authme.output.MessageKey;
import utils.FileUtils;
import utils.ToolsConstants;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Verifies that a message file has all keys as given in {@link MessageKey}.
 */
public class MessageFileVerifier {

    public static final String MESSAGES_FOLDER = ToolsConstants.MAIN_RESOURCES_ROOT + "messages/";
    private static final String NEW_LINE = "\n";

    private final String messagesFile;
    private final Map<String, String> defaultMessages;

    public MessageFileVerifier(Map<String, String> defaultMessages, String messagesFile) {
        this.messagesFile = messagesFile;
        this.defaultMessages = defaultMessages;
    }

    public void verify(boolean addMissingKeys) {
        Set<String> messageKeys = getAllMessageKeys();
        List<String> fileLines = FileUtils.readLinesFromFile(messagesFile);
        for (String line : fileLines) {
            // Skip comments and empty lines
            if (!line.startsWith("#") && !line.trim().isEmpty()) {
                handleMessagesLine(line, messageKeys);
            }
        }

        if (messageKeys.isEmpty()) {
            System.out.println("Found all message keys");
        } else {
            handleMissingKeys(messageKeys, addMissingKeys);
        }
    }

    private void handleMessagesLine(String line, Set<String> messageKeys) {
        if (line.indexOf(':') == -1) {
            System.out.println("Skipping line in unknown format: '" + line + "'");
            return;
        }

        final String key = line.substring(0, line.indexOf(':'));
        if (messageKeys.contains(key)) {
            messageKeys.remove(key);
        } else {
            System.out.println("Warning: Unknown key '" + key + "' for line '" + line + "'");
        }
    }

    private void handleMissingKeys(Set<String> missingKeys, boolean addMissingKeys) {
        for (String key : missingKeys) {
            if (addMissingKeys) {
                String defaultMessage = defaultMessages.get(key);
                FileUtils.appendToFile(messagesFile, NEW_LINE + key + ":" + defaultMessage);
                System.out.println("Added missing key '" + key + "' to file");
            } else {
                System.out.println("Error: Missing key '" + key + "'");
            }
        }
    }

    private static Set<String> getAllMessageKeys() {
        Set<String> messageKeys = new HashSet<>(MessageKey.values().length);
        for (MessageKey key : MessageKey.values()) {
            messageKeys.add(key.getKey());
        }
        return messageKeys;
    }
}
