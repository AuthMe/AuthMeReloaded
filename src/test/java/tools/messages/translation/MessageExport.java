package tools.messages.translation;

/**
 * Container class for one translatable message.
 */
public class MessageExport {

    public final String key;
    public final String tags;
    public final String defaultMessage;
    public final String translatedMessage;

    public MessageExport(String key, String[] tags, String defaultMessage, String translatedMessage) {
        this.key = key;
        this.tags = String.join(",", tags);
        this.defaultMessage = defaultMessage;
        this.translatedMessage = translatedMessage;
    }

}
