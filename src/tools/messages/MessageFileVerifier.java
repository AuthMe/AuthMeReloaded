package messages;

import fr.xephi.authme.output.MessageKey;
import utils.FileUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Verifies a message file's keys to ensure that it is in sync with {@link MessageKey}, i.e. that the file contains
 * all keys and that it doesn't have any unknown ones.
 */
public class MessageFileVerifier {

    private static final char NEW_LINE = '\n';

    private final String messagesFile;
    private final Set<String> unknownKeys = new HashSet<>();
    // Map with the missing key and a boolean indicating whether or not it was added to the file by this object
    private final Map<String, Boolean> missingKeys = new HashMap<>();

    /**
     * Create a verifier that verifies the given messages file.
     *
     * @param messagesFile The messages file to process
     */
    public MessageFileVerifier(String messagesFile) {
        this.messagesFile = messagesFile;
        verifyKeys();
    }

    /**
     * Return the list of unknown keys, i.e. the list of keys present in the file that are not
     * part of the {@link MessageKey} enum.
     *
     * @return List of unknown keys
     */
    public Set<String> getUnknownKeys() {
        return unknownKeys;
    }

    /**
     * Return the list of missing keys, i.e. all keys that are part of {@link MessageKey} but absent
     * in the messages file.
     *
     * @return The list of missing keys in the file
     */
    public Map<String, Boolean> getMissingKeys() {
        return missingKeys;
    }

    private void verifyKeys() {
        Set<String> messageKeys = getAllMessageKeys();
        List<String> fileLines = FileUtils.readLinesFromFile(messagesFile);
        for (String line : fileLines) {
            // Skip comments and empty lines
            if (!line.startsWith("#") && !line.trim().isEmpty()) {
                processKeyInFile(line, messageKeys);
            }
        }

        // All keys that remain are keys that are absent in the file
        for (String missingKey : messageKeys) {
            missingKeys.put(missingKey, false);
        }
    }

    private void processKeyInFile(String line, Set<String> messageKeys) {
        if (line.indexOf(':') == -1) {
            System.out.println("Skipping line in unknown format: '" + line + "'");
            return;
        }

        final String key = line.substring(0, line.indexOf(':'));
        if (messageKeys.contains(key)) {
            messageKeys.remove(key);
        } else {
            unknownKeys.add(key);
        }
    }

    /**
     * Add missing keys to the file with the provided default (English) message.
     *
     * @param defaultMessages The collection of default messages
     */
    public void addMissingKeys(Map<String, String> defaultMessages) {
        List<String> keysToAdd = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : missingKeys.entrySet()) {
            if (Boolean.FALSE.equals(entry.getValue()) && defaultMessages.get(entry.getKey()) != null) {
                keysToAdd.add(entry.getKey());
            }
        }

        // Very ugly way of verifying if the last char in the file is a new line, in which case we won't start by
        // adding a new line to the file. It's grossly inefficient but with the scale of the messages file it's fine
        String fileContents = FileUtils.readFromFile(messagesFile);
        String contentsToAdd = "";
        if (fileContents.charAt(fileContents.length() - 1) == NEW_LINE) {
            contentsToAdd += NEW_LINE;
        }

        // We know that all keys in keysToAdd are safe to retrieve and add
        for (String keyToAdd : keysToAdd) {
            contentsToAdd += keyToAdd + ":" + defaultMessages.get(keyToAdd) + NEW_LINE;
            missingKeys.put(keyToAdd, true);
        }
        FileUtils.appendToFile(messagesFile, contentsToAdd);
    }

    private static Set<String> getAllMessageKeys() {
        Set<String> messageKeys = new HashSet<>(MessageKey.values().length);
        for (MessageKey key : MessageKey.values()) {
            messageKeys.add(key.getKey());
        }
        return messageKeys;
    }
}
