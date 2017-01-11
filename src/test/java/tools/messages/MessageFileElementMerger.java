package tools.messages;

import fr.xephi.authme.message.MessageKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Creates the same order of message file elements as the given default message elements,
 * using the local file's own elements as much as possible and filling in elements from the
 * default messages file where necessary.
 * <p>
 * The current implementation (of this merger and of the {@link MessageFileElementReader reader})
 * has the following limitations:
 * <ul>
 *   <li>It assumes that new comments are only ever added to the bottom of the default file.</li>
 *   <li>If a file only has a partial number of the comments present in the default messages file,
 *       the file's comments will be moved to the top. This most likely adds the comment above the
 *       wrong group of messages.</li>
 *   <li>Assumes that the text for a message only takes one line.</li>
 *   <li>Ignores the last comment section of a file if it is not followed by any message entry.</li>
 * </ul>
 */
public class MessageFileElementMerger {

    /** Ordered list of comments in the messages file. */
    private final List<MessageFileComments> comments;
    /** List of message entries by corresponding MessageKey. */
    private final Map<MessageKey, MessageFileEntry> entries;
    /**
     * Ordered list of file elements of the default file. The entries of the (non-default) messages
     * file are based on this.
     */
    private final List<MessageFileElement> defaultFileElements;
    /** Missing tags in message entries. */
    private final Map<MessageKey, Collection<String>> missingTags;
    /** Counter for encountered comment elements. */
    private int commentsCounter = 0;

    private MessageFileElementMerger(List<MessageFileElement> defaultFileElements,
                                     List<MessageFileComments> comments,
                                     Map<MessageKey, MessageFileEntry> entries,
                                     Map<MessageKey, Collection<String>> missingTags) {
        this.defaultFileElements = defaultFileElements;
        this.comments = comments;
        this.entries = entries;
        this.missingTags = missingTags;
    }

    /**
     * Returns a list of file elements that follow the order and type of the provided default file elements.
     * In other words, using the list of default file elements as template and fallback, it returns the provided
     * file elements in the same order and fills in default file elements if an equivalent in {@code fileElements}
     * is not present.
     *
     * @param fileElements file elements to sort and merge
     * @param defaultFileElements file elements of the default file to base the operation on
     * @param missingTags list of missing tags per message key
     * @return ordered and complete list of file elements
     */
    public static List<MessageFileElement> mergeElements(List<MessageFileElement> fileElements,
                                                         List<MessageFileElement> defaultFileElements,
                                                         Map<MessageKey, Collection<String>> missingTags) {
        List<MessageFileComments> comments = filteredStream(fileElements, MessageFileComments.class)
            .collect(Collectors.toList());
        Map<MessageKey, MessageFileEntry> entries = filteredStream(fileElements, MessageFileEntry.class)
            .collect(Collectors.toMap(MessageFileEntry::getMessageKey, Function.identity(), (e1, e2) -> e1));

        MessageFileElementMerger merger = new MessageFileElementMerger(
            defaultFileElements, comments, entries, missingTags);
        return merger.mergeElements();
    }

    private List<MessageFileElement> mergeElements() {
        List<MessageFileElement> mergedElements = new ArrayList<>(defaultFileElements.size());
        for (MessageFileElement element : defaultFileElements) {
            if (element instanceof MessageFileComments) {
                mergedElements.add(getCommentsEntry((MessageFileComments) element));
            } else if (element instanceof MessageFileEntry) {
                mergedElements.add(getEntryForDefaultMessageEntry((MessageFileEntry) element));
            } else {
                throw new IllegalStateException("Found element of unknown subtype '" + element.getClass() + "'");
            }
        }
        return mergedElements;
    }

    private MessageFileComments getCommentsEntry(MessageFileComments defaultComments) {
        if (comments.size() > commentsCounter) {
            MessageFileComments localComments = comments.get(commentsCounter);
            ++commentsCounter;
            return localComments;
        }
        return defaultComments;
    }

    private MessageFileElement getEntryForDefaultMessageEntry(MessageFileEntry entry) {
        MessageKey messageKey = entry.getMessageKey();
        if (messageKey == null) {
            throw new IllegalStateException("Default message file should not have unknown entries, but "
                + " entry with lines '" + entry.getLines() + "' has message key = null");
        }

        MessageFileEntry localEntry = entries.get(messageKey);
        if (localEntry == null) {
            return entry.convertToMissingEntryComment();
        }

        Collection<String> absentTags = missingTags.get(messageKey);
        return absentTags == null
            ? localEntry
            : localEntry.convertToEntryWithMissingTagsComment(absentTags);
    }

    /**
     * Creates a stream of the entries in {@code collection} with only the elements which are of type {@code clazz}.
     *
     * @param collection the collection to stream over
     * @param clazz the class to restrict the elements to
     * @param <P> the collection type (parent)
     * @param <C> the type to restrict to (child)
     * @return stream over all elements of the given type
     */
    private static <P, C extends P> Stream<C> filteredStream(Collection<P> collection, Class<C> clazz) {
        return collection.stream().filter(clazz::isInstance).map(clazz::cast);
    }
}
