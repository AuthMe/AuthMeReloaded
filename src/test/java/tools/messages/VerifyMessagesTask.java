package tools.messages;

import com.google.common.collect.Multimap;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import tools.utils.ToolTask;
import tools.utils.ToolsConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Task to verify the keys in the messages files.
 */
public final class VerifyMessagesTask implements ToolTask {

    /** The folder containing the message files. */
    private static final String MESSAGES_FOLDER = ToolsConstants.MAIN_RESOURCES_ROOT + "messages/";
    /** Pattern of the message file names. */
    private static final Pattern MESSAGE_FILE_PATTERN = Pattern.compile("messages_[a-z]{2,7}\\.yml");
    /** File to get default messages from (assumes that it is complete). */
    private static final String DEFAULT_MESSAGES_FILE = MESSAGES_FOLDER + "messages_en.yml";

    @Override
    public String getTaskName() {
        return "verifyMessages";
    }

    @Override
    public void execute(Scanner scanner) {
        System.out.println("Check a specific file only?");
        System.out.println("Enter the language code for a specific file (e.g. 'es' for messages_es.yml)");
        System.out.println("- Empty line will check all files in the resources messages folder (default)");
        String inputFile = scanner.nextLine();

        System.out.println("Add any missing keys to files? ['y' = yes]");
        boolean addMissingKeys = "y".equalsIgnoreCase(scanner.nextLine());

        List<File> messageFiles;
        if (StringUtils.isEmpty(inputFile)) {
            messageFiles = getMessagesFiles();
        } else {
            File customFile = new File(MESSAGES_FOLDER, "messages_" + inputFile + ".yml");
            messageFiles = Collections.singletonList(customFile);
        }

        FileConfiguration defaultMessages = null;
        if (addMissingKeys) {
            defaultMessages = YamlConfiguration.loadConfiguration(new File(DEFAULT_MESSAGES_FILE));
        }

        // Verify the given files
        for (File file : messageFiles) {
            System.out.println("Verifying '" + file.getName() + "'");
            MessageFileVerifier verifier = new MessageFileVerifier(file);
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
        List<MissingKey> missingKeys = verifier.getMissingKeys();
        if (!missingKeys.isEmpty()) {
            System.out.println("  Missing keys: " + missingKeys);
        }

        Set<String> unknownKeys = verifier.getUnknownKeys();
        if (!unknownKeys.isEmpty()) {
            System.out.println("  Unknown keys: " + unknownKeys);
        }

        Multimap<String, String> missingTags = verifier.getMissingTags();
        for (Map.Entry<String, String> entry : missingTags.entries()) {
            System.out.println("  Missing tag '" + entry.getValue() + "' in entry with key '" + entry.getKey() + "'");
        }
    }

    public static void verifyFileAndAddKeys(MessageFileVerifier verifier, FileConfiguration defaultMessages) {
        List<MissingKey> missingKeys = verifier.getMissingKeys();
        if (!missingKeys.isEmpty() || !verifier.getMissingTags().isEmpty()) {
            verifier.addMissingKeys(defaultMessages);
            List<String> addedKeys = getMissingKeysWithAdded(missingKeys, true);
            System.out.println("  Added missing keys " + addedKeys);

            List<String> unsuccessfulKeys = getMissingKeysWithAdded(missingKeys, false);
            if (!unsuccessfulKeys.isEmpty()) {
                System.err.println("  Warning! Could not add all missing keys (problem with loading " +
                    "default messages?)");
                System.err.println("  Could not add keys " + unsuccessfulKeys);
            }
        }

        Set<String> unknownKeys = verifier.getUnknownKeys();
        if (!unknownKeys.isEmpty()) {
            System.out.println("  Unknown keys: " + unknownKeys);
        }

        Multimap<String, String> missingTags = verifier.getMissingTags();
        for (Map.Entry<String, String> entry : missingTags.entries()) {
            System.out.println("  Missing tag '" + entry.getValue() + "' in entry with key '" + entry.getKey() + "'");
        }
    }

    private static List<String> getMissingKeysWithAdded(List<MissingKey> missingKeys, boolean wasAdded) {
        return missingKeys.stream()
            .filter(e -> e.getWasAdded() == wasAdded)
            .map(MissingKey::getKey)
            .collect(Collectors.toList());
    }

    private static List<File> getMessagesFiles() {
        File folder = new File(MESSAGES_FOLDER);
        File[] files = folder.listFiles();
        if (files == null) {
            throw new IllegalStateException("Could not read files from folder '" + folder.getName() + "'");
        }

        List<File> messageFiles = new ArrayList<>();
        for (File file : files) {
            if (MESSAGE_FILE_PATTERN.matcher(file.getName()).matches()) {
                messageFiles.add(file);
            }
        }
        if (messageFiles.isEmpty()) {
            throw new IllegalStateException("Error getting message files: list of files is empty");
        }
        return messageFiles;
    }
}
