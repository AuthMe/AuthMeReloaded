package tools.messages.translation;

import com.google.common.io.Resources;
import com.google.gson.Gson;
import fr.xephi.authme.message.MessageKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import tools.messages.MessageFileVerifier;
import tools.messages.VerifyMessagesTask;
import tools.utils.FileIoUtils;
import tools.utils.ToolTask;
import tools.utils.ToolsConstants;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
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
    private Set<String> messageCodes;

    @Override
    public String getTaskName() {
        return "importMessages";
    }

    @Override
    public void execute(Scanner scanner) {
        System.out.println("Enter URL to import from");
        // Dirty trick: replace https:// with http:// so we don't have to worry about installing certificates...
        String url = scanner.nextLine().replace("https://", "http://");

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
            String json = Resources.toString(url, StandardCharsets.UTF_8);
            return gson.fromJson(json, LanguageExport.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void mergeExportIntoFile(LanguageExport export) {
        String languageCode = export.code;
        String fileName = MESSAGES_FOLDER + "messages_" + languageCode + ".yml";
        File file = new File(fileName);
        FileConfiguration fileConfiguration;
        if (file.exists()) {
            removeAllTodoComments(fileName);
            fileConfiguration = AuthMeYamlConfiguration.loadConfiguration(file);
        } else {
            fileConfiguration = new AuthMeYamlConfiguration();
        }

        buildMessageCodeList();
        for (MessageExport messageExport : export.messages) {
            if (!messageCodes.contains(messageExport.key)) {
                throw new IllegalStateException("Message key '" + messageExport.key + "' does not exist");
            } else if (!messageExport.translatedMessage.isEmpty()) {
                fileConfiguration.set(messageExport.key, messageExport.translatedMessage);
            }
        }
        try {
            fileConfiguration.save(file);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        MessageFileVerifier verifier = new MessageFileVerifier(file);
        VerifyMessagesTask.verifyFileAndAddKeys(verifier, YamlConfiguration.loadConfiguration(
            new File(MESSAGES_FOLDER + "messages_en.yml")));
    }

    private void buildMessageCodeList() {
        messageCodes = new HashSet<>(MessageKey.values().length);
        for (MessageKey messageKey : MessageKey.values()) {
            messageCodes.add(messageKey.getKey());
        }
    }

    /**
     * Removes all to-do comments written by {@link VerifyMessagesTask}. This is helpful as the YamlConfiguration
     * moves those comments otherwise upon saving.
     *
     * @param file The file whose to-do comments should be removed
     */
    private static void removeAllTodoComments(String file) {
        String contents = FileIoUtils.readFromFile(file);
        String regex = "^# TODO .*$";
        contents = Pattern.compile(regex, Pattern.MULTILINE).matcher(contents).replaceAll("");
        FileIoUtils.writeToFile(file, contents);
    }
}
