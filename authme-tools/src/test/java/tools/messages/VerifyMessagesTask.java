package tools.messages;

import com.google.common.collect.Multimap;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.MessagePathHelper;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import tools.utils.ToolTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import static fr.xephi.authme.message.MessagePathHelper.DEFAULT_MESSAGES_FILE;
import static tools.utils.FileIoUtils.listFilesOrThrow;
import static tools.utils.ToolsConstants.MAIN_RESOURCES_ROOT;

/**
 * Task to verify the keys in the messages files.
 */
public final class VerifyMessagesTask implements ToolTask {

    /** The folder containing the message files. */
    private static final String MESSAGES_FOLDER = MAIN_RESOURCES_ROOT + MessagePathHelper.MESSAGES_FOLDER;

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
        if (StringUtils.isBlank(inputFile)) {
            messageFiles = getMessagesFiles();
        } else {
            File customFile = new File(MAIN_RESOURCES_ROOT, MessagePathHelper.createMessageFilePath(inputFile));
            messageFiles = Collections.singletonList(customFile);
        }

        FileConfiguration defaultFileConfiguration = null;
        if (addMissingKeys) {
            defaultFileConfiguration = YamlConfiguration.loadConfiguration(
                new File(MAIN_RESOURCES_ROOT, DEFAULT_MESSAGES_FILE));
        }

        // Verify the given files
        for (File file : messageFiles) {
            System.out.println("Verifying '" + file.getName() + "'");
            MessageFileVerifier verifier = new MessageFileVerifier(file);
            if (addMissingKeys) {
                outputVerificationResults(verifier);
                updateMessagesFile(file, verifier, defaultFileConfiguration);
            } else {
                outputVerificationResults(verifier);
            }
        }

        if (messageFiles.size() > 1) {
            System.out.println("Checked " + messageFiles.size() + " files");
        }
    }

    /**
     * Outputs the verification results to the console.
     *
     * @param verifier the verifier whose results should be output
     */
    private static void outputVerificationResults(MessageFileVerifier verifier) {
        Set<MessageKey> missingKeys = verifier.getMissingKeys();
        if (!missingKeys.isEmpty()) {
            System.out.println("  Missing keys: " + missingKeys);
        }

        Set<String> unknownKeys = verifier.getUnknownKeys();
        if (!unknownKeys.isEmpty()) {
            System.out.println("  Unknown keys: " + unknownKeys);
        }

        Multimap<MessageKey, String> missingTags = verifier.getMissingTags();
        for (Map.Entry<MessageKey, String> entry : missingTags.entries()) {
            System.out.println("  Missing tag '" + entry.getValue() + "' in entry '" + entry.getKey() + "'");
        }
    }

    /**
     * Updates a messages file to have the same order as the default file and to contain all
     * failed verifications as comments.
     *
     * @param file the file to update
     * @param verifier the verifier whose results should be used
     * @param defaultConfiguration default file configuration to retrieve missing texts from
     */
    private static void updateMessagesFile(File file, MessageFileVerifier verifier,
                                           FileConfiguration defaultConfiguration) {
        if (verifier.hasErrors()) {
            MessagesFileWriter.writeToFileWithCommentsFromDefault(file, defaultConfiguration);
        }
    }


    private static List<File> getMessagesFiles() {
        File[] files = listFilesOrThrow(new File(MESSAGES_FOLDER));
        List<File> messageFiles = new ArrayList<>();
        for (File file : files) {
            if (MessagePathHelper.isMessagesFile(file.getName())) {
                messageFiles.add(file);
            }
        }
        if (messageFiles.isEmpty()) {
            throw new IllegalStateException("Error getting message files: list of files is empty");
        }
        return messageFiles;
    }
}
