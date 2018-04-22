package fr.xephi.authme.message;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.command.help.HelpSection;
import fr.xephi.authme.util.ExceptionUtils;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.fail;
import static tools.utils.FileIoUtils.listFilesOrThrow;

/**
 * Tests that all YML text files can be loaded.
 */
public class YamlTextFileCheckerTest {

    /** Path in the resources folder where the message files are located. */
    private static final String MESSAGES_FOLDER = "/messages/";
    /** Contains all files of the MESSAGES_FOLDER. */
    private static List<File> messageFiles;

    @BeforeClass
    public static void loadMessagesFiles() {
        File folder = TestHelper.getJarFile(MESSAGES_FOLDER);
        messageFiles = Arrays.asList(listFilesOrThrow(folder));
    }

    @Test
    public void testAllMessagesYmlFiles() {
        checkFiles(
            Pattern.compile("messages_\\w+\\.yml"),
            MessageKey.LOGIN_MESSAGE.getKey());
    }

    @Test
    public void testAllHelpYmlFiles() {
        checkFiles(
            Pattern.compile("help_\\w+\\.yml"),
            HelpSection.ALTERNATIVES.getKey());
    }

    /**
     * Checks all files in the messages folder that match the given pattern.
     *
     * @param pattern the pattern the file name needs to match
     * @param mandatoryKey key present in all matched files
     */
    private void checkFiles(Pattern pattern, String mandatoryKey) {
        List<String> errors = new ArrayList<>();

        boolean hasMatch = false;
        for (File file : messageFiles) {
            if (pattern.matcher(file.getName()).matches()) {
                checkFile(file, mandatoryKey, errors);
                hasMatch = true;
            }
        }

        if (!errors.isEmpty()) {
            fail("Errors while checking files matching '" + pattern + "':\n-" + String.join("\n-", errors));
        } else if (!hasMatch) {
            fail("Could not find any files satisfying pattern '" + pattern + "'");
        }
    }

    /**
     * Checks that the provided YAML file can be loaded and that it contains a non-empty text
     * for the provided mandatory key.
     *
     * @param file the file to check
     * @param mandatoryKey the key for which text must be present
     * @param errors collection of errors to add to if the verification fails
     */
    private void checkFile(File file, String mandatoryKey, List<String> errors) {
        try {
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
            if (StringUtils.isEmpty(configuration.getString(mandatoryKey))) {
                errors.add("Message for '" + mandatoryKey + "' is empty");
            }
        } catch (Exception e) {
            errors.add("Could not load file: " + ExceptionUtils.formatException(e));
        }
    }
}
