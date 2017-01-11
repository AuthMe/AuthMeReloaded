package tools.messages;

import java.util.Collections;
import java.util.List;

/**
 * An element (a logical unit) in a messages file.
 */
public abstract class MessageFileElement {

    private final List<String> lines;

    protected MessageFileElement(List<String> lines) {
        this.lines = Collections.unmodifiableList(lines);
    }

    public List<String> getLines() {
        return lines;
    }
}
