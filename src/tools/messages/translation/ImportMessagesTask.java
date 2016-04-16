package messages.translation;

import com.google.common.io.Resources;
import com.google.gson.Gson;
import messages.MessageFileVerifier;
import messages.VerifyMessagesTask;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import utils.FileUtils;
import utils.ToolTask;
import utils.ToolsConstants;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Imports a message file from a remote JSON export and validates the resulting file.
 * <p>
 * Comments at the top of an existing file should remain after the import, but it is important
 * to verify that no unwanted changes have been applied to the file. Note that YAML comments
 * tend to disappear if there is no space between the <code>#</code> and the first character.
 */
public class ImportMessagesTask implements ToolTask {

    private static final String MESSAGES_FOLDER = ToolsConstants.MAIN_RESOURCES_ROOT + "messages/";
    private Gson gson = new Gson();

    @Override
    public String getTaskName() {
        return "importMessages";
    }

    @Override
    public void execute(Scanner scanner) {
        System.out.println("Enter URL to import from");
        String url = scanner.nextLine();

        LanguageExport languageExport = getLanguageExportFromUrl(url);
        if (languageExport == null) {
            throw new IllegalStateException("An error occurred: constructed language export is null");
        }

        mergeExportIntoFile(languageExport);
        System.out.println("Saved to messages file for code '" + languageExport.code + "'");
    }

    private LanguageExport getLanguageExportFromUrl(String location) {
        try {
            URL url = new URL(location);
            String json = Resources.toString(url, Charset.forName("UTF-8"));
            return gson.fromJson(json, LanguageExport.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void mergeExportIntoFile(LanguageExport export) {
        String languageCode = export.code;
        String fileName = MESSAGES_FOLDER + "messages_" + languageCode + ".yml";
        File file = new File(fileName);
        if (!file.exists()) {
            throw new IllegalStateException("Messages file for language code " + languageCode + " does not exist");
        }
        removeAllTodoComments(fileName);

        FileConfiguration fileConfiguration = AuthMeYamlConfiguration.loadConfiguration(file);
        for (MessageExport messageExport : export.messages) {
            if (!messageExport.translatedMessage.isEmpty()) {
                fileConfiguration.set(messageExport.key, messageExport.translatedMessage);
            }
        }
        try {
            fileConfiguration.save(file);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        MessageFileVerifier verifier = new MessageFileVerifier(fileName);
        VerifyMessagesTask.verifyFileAndAddKeys(verifier, YamlConfiguration.loadConfiguration(
            new File(MESSAGES_FOLDER + "messages_en.yml")));
    }

    /**
     * Removes all to-do comments written by {@link VerifyMessagesTask}. This is helpful as the YamlConfiguration
     * moves those comments otherwise upon saving.
     *
     * @param file The file whose to-do comments should be removed
     */
    private static void removeAllTodoComments(String file) {
        String contents = FileUtils.readFromFile(file);
        String regex = "^# TODO .*$";
        contents = Pattern.compile(regex, Pattern.MULTILINE).matcher(contents).replaceAll("");
        FileUtils.writeToFile(file, contents);
    }
}
