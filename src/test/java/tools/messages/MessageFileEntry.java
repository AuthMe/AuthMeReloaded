package tools.messages;

import fr.xephi.authme.message.MessageKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

/**
 * Entry in a message file for a message key.
 */
public class MessageFileEntry extends MessageFileElement {

    private static final Pattern MESSAGE_ENTRY_REGEX = Pattern.compile("([a-zA-Z_-]+): .*");
    private final MessageKey messageKey;

    public MessageFileEntry(String line) {
        this(singletonList(line), extractMessageKey(line));
    }

    private MessageFileEntry(List<String> lines, MessageKey messageKey) {
        super(lines);
        this.messageKey = messageKey;
    }

    public static boolean isMessageEntry(String line) {
        return MESSAGE_ENTRY_REGEX.matcher(line).matches();
    }

    public MessageKey getMessageKey() {
        return messageKey;
    }

    /**
     * Based on this entry, creates a comments element indicating that this message is missing.
     *
     * @return comments element based on this message element
     */
    public MessageFileComments convertToMissingEntryComment() {
        List<String> comments = getLines().stream().map(l -> "# TODO " + l).collect(Collectors.toList());
        return new MessageFileComments(comments);
    }

    /**
     * Creates an adapted message file entry object with a comment for missing tags.
     *
     * @param missingTags the tags missing in the message
     * @return message file entry with verification comment
     */
    public MessageFileEntry convertToEntryWithMissingTagsComment(Collection<String> missingTags) {
        List<String> lines = new ArrayList<>(getLines().size() + 1);
        lines.add("# TODO: Missing tags " + String.join(", ", missingTags));
        lines.addAll(getLines());
        return new MessageFileEntry(lines, messageKey);
    }

    /**
     * Returns the {@link MessageKey} this entry is for. Returns {@code null} if the message key could not be matched.
     *
     * @param line the line to process
     * @return the associated message key, or {@code null} if no match was found
     */
    private static MessageKey extractMessageKey(String line) {
        Matcher matcher = MESSAGE_ENTRY_REGEX.matcher(line);
        if (matcher.find()) {
            String key = matcher.group(1);
            return fromKey(key);
        }
        throw new IllegalStateException("Could not extract message key from line '" + line + "'");
    }

    private static MessageKey fromKey(String key) {
        for (MessageKey messageKey : MessageKey.values()) {
            if (messageKey.getKey().equals(key)) {
                return messageKey;
            }
        }
        return null;
    }
}
