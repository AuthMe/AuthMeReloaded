package tools.messages;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import fr.xephi.authme.message.MessageKey;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Verifies a message file's keys to ensure that it is in sync with {@link MessageKey}, i.e. that the file contains
 * all keys and that it doesn't have any unknown ones.
 */
public class MessageFileVerifier {

    private final File messagesFile;
    private final Set<String> unknownKeys = new HashSet<>();
    private final Set<MessageKey> missingKeys = new HashSet<>();
    private final Multimap<MessageKey, String> missingTags = HashMultimap.create();

    /**
     * Create a verifier that verifies the given messages file.
     *
     * @param messagesFile The messages file to process
     */
    public MessageFileVerifier(File messagesFile) {
        Preconditions.checkArgument(messagesFile.exists(), "Message file '" + messagesFile + "' does not exist");
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
    public Set<MessageKey> getMissingKeys() {
        return missingKeys;
    }

    /**
     * Return the collection of tags the message key defines that aren't present in the read line.
     *
     * @return Collection of missing tags per message key
     */
    public Multimap<MessageKey, String> getMissingTags() {
        return missingTags;
    }

    /**
     * @return true if the verifier has found an issue with the analyzed file, false otherwise
     */
    public boolean hasErrors() {
        return !missingKeys.isEmpty() || !missingTags.isEmpty() || !unknownKeys.isEmpty();
    }

    private void verifyKeys() {
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(messagesFile);

        // Check known keys (their existence + presence of all tags)
        for (MessageKey messageKey : MessageKey.values()) {
            final String key = messageKey.getKey();
            if (configuration.isString(key)) {
                checkTagsInMessage(messageKey, configuration.getString(key));
            } else {
                missingKeys.add(messageKey);
            }
        }

        // Check FileConfiguration for all of its keys to find unknown keys
        for (String key : configuration.getValues(true).keySet()) {
            if (isNotInnerNode(key, configuration) && !messageKeyExists(key)) {
                unknownKeys.add(key);
            }
        }
    }

    private static boolean isNotInnerNode(String key, FileConfiguration configuration) {
        return !(configuration.get(key) instanceof MemorySection);
    }

    private void checkTagsInMessage(MessageKey messageKey, String message) {
        for (String tag : messageKey.getTags()) {
            if (!message.contains(tag)) {
                missingTags.put(messageKey, tag);
            }
        }
    }

    private static boolean messageKeyExists(String key) {
        for (MessageKey messageKey : MessageKey.values()) {
            if (messageKey.getKey().equals(key)) {
                return true;
            }
        }
        return false;
    }
}
