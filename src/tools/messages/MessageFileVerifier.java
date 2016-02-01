package messages;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.util.StringUtils;
import utils.FileUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Verifies a message file's keys to ensure that it is in sync with {@link MessageKey}, i.e. that the file contains
 * all keys and that it doesn't have any unknown ones.
 */
public class MessageFileVerifier {

    private static final String NEW_LINE = "\n";

    private final String messagesFile;
    private final Set<String> unknownKeys = new HashSet<>();
    // Map with the missing key and a boolean indicating whether or not it was added to the file by this object
    private final Map<String, Boolean> missingKeys = new HashMap<>();
    private final Multimap<String, String> missingTags = HashMultimap.create();

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

    /**
     * Return the collection of tags the message key defines that aren't present in the read line.
     *
     * @return Collection of missing tags per message key. Key = message key, value = missing tag.
     */
    public Multimap<String, String> getMissingTags() {
        return missingTags;
    }

    private void verifyKeys() {
        List<MessageKey> messageKeys = getAllMessageKeys();
        List<String> fileLines = readFileLines();
        for (String line : fileLines) {
            // Skip comments and empty lines
            if (!line.startsWith("#") && !line.trim().isEmpty()) {
                processKeyInFile(line, messageKeys);
            }
        }

        // All keys that remain are keys that are absent in the file
        for (MessageKey missingKey : messageKeys) {
            missingKeys.put(missingKey.getKey(), false);
        }
    }

    private void processKeyInFile(String line, List<MessageKey> messageKeys) {
        if (line.indexOf(':') == -1) {
            System.out.println("Skipping line in unknown format: '" + line + "'");
            return;
        }

        final String readKey = line.substring(0, line.indexOf(':'));
        boolean foundKey = false;
        for (Iterator<MessageKey> it = messageKeys.iterator(); it.hasNext(); ) {
            MessageKey messageKey = it.next();
            if (messageKey.getKey().equals(readKey)) {
                checkTagsInMessage(readKey, line.substring(line.indexOf(':')), messageKey.getTags());
                it.remove();
                foundKey = true;
                break;
            }
        }
        if (!foundKey) {
            unknownKeys.add(readKey);
        }
    }

    private void checkTagsInMessage(String key, String message, String[] tags) {
        for (String tag : tags) {
            if (!message.contains(tag)) {
                missingTags.put(key, tag);
            }
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

        // We know that all keys in keysToAdd are safe to retrieve and write
        StringBuilder sb = new StringBuilder(NEW_LINE);
        for (String keyToAdd : keysToAdd) {
            sb.append(keyToAdd).append(":").append(defaultMessages.get(keyToAdd)).append(NEW_LINE);
            missingKeys.put(keyToAdd, true);
        }
        FileUtils.appendToFile(messagesFile, sb.toString());
    }

    private static List<MessageKey> getAllMessageKeys() {
        return new ArrayList<>(Arrays.asList(MessageKey.values()));
    }

    /**
     * Read all lines from the messages file and skip empty lines and comment lines.
     * This method appends lines starting with two spaces to the previously read line,
     * akin to a YAML parser.
     */
    private List<String> readFileLines() {
        String[] rawLines = FileUtils.readFromFile(messagesFile).split("\\n");
        List<String> lines = new ArrayList<>();
        for (String line : rawLines) {
            // Skip comments and empty lines
            if (!line.startsWith("#") && !StringUtils.isEmpty(line)) {
                // Line is indented, i.e. it needs to be appended to the previous line
                if (line.startsWith("  ")) {
                    appendToLastElement(lines, line.substring(1));
                } else {
                    lines.add(line);
                }
            }
        }
        return lines;
    }

    private static void appendToLastElement(List<String> list, String text) {
        if (list.isEmpty()) {
            throw new IllegalStateException("List cannot be empty!");
        }
        int lastIndex = list.size() - 1;
        list.set(lastIndex, list.get(lastIndex).concat(text));
    }
}
