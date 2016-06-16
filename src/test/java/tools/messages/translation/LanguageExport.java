package tools.messages.translation;

import java.util.Collections;
import java.util.List;

/**
 * Export of a language's messages.
 */
public class LanguageExport {

    public final String code;
    public final List<MessageExport> messages;

    public LanguageExport(String code, List<MessageExport> messages) {
        this.code = code;
        this.messages = Collections.unmodifiableList(messages);
    }

}
