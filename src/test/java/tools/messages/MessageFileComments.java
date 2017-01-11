package tools.messages;

import java.util.List;

/**
 * Represents a section of one or more consecutive comment lines in a file.
 */
public class MessageFileComments extends MessageFileElement {

    public MessageFileComments(List<String> lines) {
        super(lines);
    }
}
