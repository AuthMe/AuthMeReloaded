package fr.xephi.authme.message;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper for creating and processing paths to message files.
 */
public final class MessagePathHelper {

    /** The default language (used as fallback, assumed to be complete, etc.). */
    public static final String DEFAULT_LANGUAGE = "en";
    /** Local path to the folder containing the message files. */
    public static final String MESSAGES_FOLDER = "messages/";
    /** Local path to the default messages file (messages/messages_en.yml). */
    public static final String DEFAULT_MESSAGES_FILE = createMessageFilePath(DEFAULT_LANGUAGE);

    private static final Pattern MESSAGE_FILE_PATTERN = Pattern.compile("messages_([a-z]+)\\.yml");
    private static final Pattern HELP_MESSAGES_FILE = Pattern.compile("help_[a-z]+\\.yml");

    private MessagePathHelper() {
    }

    /**
     * Creates the local path to the messages file for the provided language code.
     *
     * @param languageCode the language code
     * @return local path to the messages file of the given language
     */
    public static String createMessageFilePath(String languageCode) {
        return "messages/messages_" + languageCode + ".yml";
    }

    /**
     * Creates the local path to the help messages file for the provided language code.
     *
     * @param languageCode the language code
     * @return local path to the help messages file of the given language
     */
    public static String createHelpMessageFilePath(String languageCode) {
        return "messages/help_" + languageCode + ".yml";
    }

    /**
     * Returns whether the given file name is a messages file.
     *
     * @param filename the file name to test
     * @return true if it is a messages file, false otherwise
     */
    public static boolean isMessagesFile(String filename) {
        return MESSAGE_FILE_PATTERN.matcher(filename).matches();
    }

    /**
     * Returns the language code the given file name is for if it is a messages file, otherwise null is returned.
     *
     * @param filename the file name to process
     * @return the language code the file name is a messages file for, or null if not applicable
     */
    public static String getLanguageIfIsMessagesFile(String filename) {
        Matcher matcher = MESSAGE_FILE_PATTERN.matcher(filename);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Returns whether the given file name is a help messages file.
     *
     * @param filename the file name to test
     * @return true if it is a help messages file, false otherwise
     */
    public static boolean isHelpFile(String filename) {
        return HELP_MESSAGES_FILE.matcher(filename).matches();
    }
}
