package messages;

import fr.xephi.authme.util.StringUtils;
import utils.FileUtils;
import utils.ToolTask;
import utils.ToolsConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * Task to verify the keys in the messages files.
 */
public final class VerifyMessagesTask implements ToolTask {

    /** The folder containing the message files. */
    private static final String MESSAGES_FOLDER = ToolsConstants.MAIN_RESOURCES_ROOT + "messages/";
    /** Pattern of the message file names. */
    private static final Pattern MESSAGE_FILE_PATTERN = Pattern.compile("messages_[a-z]{2,7}\\.yml");
    /** Tag that is replaced to the messages folder in user input. */
    private static final String SOURCES_TAG = "{msgdir}";

    @Override
    public String getTaskName() {
        return "verifyMessages";
    }

    @Override
    public void execute(Scanner scanner) {
        System.out.println("Check a specific file only?");
        System.out.println("- Empty line will check all files in the resources messages folder (default)");
        System.out.println(format("- %s will be replaced to the messages folder %s", SOURCES_TAG, MESSAGES_FOLDER));
        String inputFile = scanner.nextLine();

        System.out.println("Add any missing keys to files? ['y' = yes]");
        boolean addMissingKeys = "y".equals(scanner.nextLine());

        // Set up needed objects
        Map<String, String> defaultMessages = null;
        if (addMissingKeys) {
            defaultMessages = constructDefaultMessages();
        }

        List<File> messageFiles;
        if (StringUtils.isEmpty(inputFile)) {
            messageFiles = getMessagesFiles();
        } else {
            File customFile = new File(inputFile.replace(SOURCES_TAG, MESSAGES_FOLDER));
            messageFiles = Collections.singletonList(customFile);
        }

        // Verify the given files
        for (File file : messageFiles) {
            System.out.println("Verifying '" + file.getName() + "'");
            MessageFileVerifier verifier = new MessageFileVerifier(file.getAbsolutePath());
            if (addMissingKeys) {
                verifyFileAndAddKeys(verifier, defaultMessages);
            } else {
                verifyFile(verifier);
            }
        }

        if (messageFiles.size() > 1) {
            System.out.println("Checked " + messageFiles.size() + " files");
        }
    }

    private static void verifyFile(MessageFileVerifier verifier) {
        Map<String, Boolean> missingKeys = verifier.getMissingKeys();
        if (!missingKeys.isEmpty()) {
            System.out.println("  Missing keys: " + missingKeys.keySet());
        }

        Set<String> unknownKeys = verifier.getUnknownKeys();
        if (!unknownKeys.isEmpty()) {
            System.out.println("  Unknown keys: " + unknownKeys);
        }
    }

    private static void verifyFileAndAddKeys(MessageFileVerifier verifier, Map<String, String> defaultMessages) {
        Map<String, Boolean> missingKeys = verifier.getMissingKeys();
        if (!missingKeys.isEmpty()) {
            verifier.addMissingKeys(defaultMessages);
            missingKeys = verifier.getMissingKeys();
            List<String> addedKeys = getKeysWithValue(Boolean.TRUE, missingKeys);
            System.out.println("  Added missing keys " + addedKeys);

            List<String> unsuccessfulKeys = getKeysWithValue(Boolean.FALSE, missingKeys);
            if (!unsuccessfulKeys.isEmpty()) {
                System.out.println("  Warning! Could not add all missing keys (problem with loading " +
                    "default messages?)");
                System.out.println("  Could not add keys " + unsuccessfulKeys);
            }
        }

        Set<String> unknownKeys = verifier.getUnknownKeys();
        if (!unknownKeys.isEmpty()) {
            System.out.println("  Unknown keys: " + unknownKeys);
        }
    }

    private static Map<String, String> constructDefaultMessages() {
        String defaultMessagesFile = MESSAGES_FOLDER + "messages_en.yml";
        List<String> lines = FileUtils.readLinesFromFile(defaultMessagesFile);
        Map<String, String> messages = new HashMap<>(lines.size());
        for (String line : lines) {
            if (line.startsWith("#") || line.trim().isEmpty()) {
                continue;
            }
            if (line.indexOf(':') == -1 || line.indexOf(':') == line.length() - 1) {
                System.out.println("Warning! Unknown format in default messages file for line '" + line + "'");
            } else {
                String key = line.substring(0, line.indexOf(':'));
                messages.put(key, line.substring(line.indexOf(':') + 1));
            }
        }
        return messages;
    }

    private static <K, V> List<K> getKeysWithValue(V value, Map<K, V> map) {
        List<K> result = new ArrayList<>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (value.equals(entry.getValue())) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    private static List<File> getMessagesFiles() {
        File folder = new File(MESSAGES_FOLDER);
        File[] files = folder.listFiles();
        if (files == null) {
            throw new RuntimeException("Could not read files from folder '" + folder.getName() + "'");
        }

        List<File> messageFiles = new ArrayList<>();
        for (File file : files) {
            if (MESSAGE_FILE_PATTERN.matcher(file.getName()).matches()) {
                messageFiles.add(file);
            }
        }
        if (messageFiles.isEmpty()) {
            throw new RuntimeException("Error getting message files: list of files is empty");
        }
        return messageFiles;
    }
}
