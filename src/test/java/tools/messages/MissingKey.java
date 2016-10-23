package tools.messages;

/**
 * Missing message key in a file.
 */
public class MissingKey {

    private final String key;
    private boolean wasAdded;

    public MissingKey(String key) {
        this.key = key;
    }

    /**
     * @return the message key that is missing
     */
    public String getKey() {
        return key;
    }

    public void setWasAdded(boolean wasAdded) {
        this.wasAdded = wasAdded;
    }

    /**
     * @return true if a comment was added to the file, false otherwise
     */
    public boolean getWasAdded() {
        return wasAdded;
    }

    @Override
    public String toString() {
        return key;
    }
}
