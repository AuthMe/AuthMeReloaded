package messages;

import utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * TODO: ljacqu write JavaDoc
 */
public final class MessagesVerifierRunner {

    private MessagesVerifierRunner() {
    }

    public static void main(String[] args) {
        final Map<String, String> defaultMessages = constructDefaultMessages();
        final List<File> messagesFiles = getMessagesFiles();

        if (messagesFiles.isEmpty()) {
            throw new RuntimeException("Error getting messages file: list of files is empty");
        }

        for (File file : messagesFiles) {
            System.out.println("Verifying '" + file.getName() + "'");
            MessageFileVerifier messageFileVerifier = new MessageFileVerifier(defaultMessages, file.getAbsolutePath());
            messageFileVerifier.verify(false);
            System.out.println();
        }
    }

    private static Map<String, String> constructDefaultMessages() {
        String defaultMessagesFile = MessageFileVerifier.MESSAGES_FOLDER + "messages_en.yml";
        List<String> lines = FileUtils.readLinesFromFile(defaultMessagesFile);
        Map<String, String> messages = new HashMap<>(lines.size());
        for (String line : lines) {
            if (line.startsWith("#") || line.trim().isEmpty()) {
                continue;
            }
            if (line.indexOf(':') == -1) {
                System.out.println("Warning! Unknown format in default messages file for line '" + line + "'");
            } else {
                String key = line.substring(0, line.indexOf(':'));
                messages.put(key, line.substring(line.indexOf(':') + 1)); // fixme: may throw exception
            }
        }
        return messages;
    }

    private static List<File> getMessagesFiles() {
        final Pattern messageFilePattern = Pattern.compile("messages_[a-z]{2,3}\\.yml");
        File folder = new File(MessageFileVerifier.MESSAGES_FOLDER);
        File[] files = folder.listFiles();
        if (files == null) {
            throw new RuntimeException("Could not read files from folder '" + folder.getName() + "'");
        }

        List<File> messageFiles = new ArrayList<>();
        for (File file : files) {
            if (messageFilePattern.matcher(file.getName()).matches()) {
                messageFiles.add(file);
            }
        }
        return messageFiles;
    }
}
