package tools.messages;

import tools.utils.FileIoUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Reads a messages file and returns the lines as corresponding {@link MessageFileElement} objects.
 *
 * @see MessageFileElementMerger
 */
public class MessageFileElementReader {

    private final List<MessageFileElement> elements = new ArrayList<>();

    private MessageFileElementReader() {
    }

    /**
     * Returns the message files as separate {@link MessageFileElement elements}.
     *
     * @param file the file to read
     * @return the file's elements
     */
    public static List<MessageFileElement> readFileIntoElements(File file) {
        checkArgument(file.exists(), "Template file '" + file + "' must exist");

        MessageFileElementReader reader = new MessageFileElementReader();
        reader.loadElements(file.toPath());
        return reader.elements;
    }

    private void loadElements(Path path) {
        List<String> currentCommentSection = new ArrayList<>(10);
        for (String line : FileIoUtils.readLinesFromFile(path)) {
            if (isTodoComment(line)) {
                continue;
            }

            if (isCommentLine(line)) {
                currentCommentSection.add(line);
            } else if (MessageFileEntry.isMessageEntry(line)) {
                if (!currentCommentSection.isEmpty()) {
                    processTempCommentsList(currentCommentSection);
                }
                elements.add(new MessageFileEntry(line));
            } else {
                throw new IllegalStateException("Could not match line '" + line + "' to any type");
            }
        }
    }

    /**
     * Creates a message file comments element for one or more read comment lines. Does not add
     * a comments element if the read lines are only empty lines.
     *
     * @param comments the read comment lines
     */
    private void processTempCommentsList(List<String> comments) {
        if (comments.stream().anyMatch(c -> !c.trim().isEmpty())) {
            elements.add(new MessageFileComments(new ArrayList<>(comments)));
        }
        comments.clear();
    }

    private static boolean isCommentLine(String line) {
        return line.trim().isEmpty() || line.trim().startsWith("#");
    }

    private static boolean isTodoComment(String line) {
        return line.startsWith("# TODO ");
    }
}
