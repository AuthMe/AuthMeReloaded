package messages.translation;

import com.google.common.io.Resources;
import com.google.gson.Gson;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.yaml.snakeyaml.DumperOptions;
import utils.ToolTask;
import utils.ToolsConstants;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Scanner;

/**
 * Imports a message file from a JSON export.
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
        System.out.println("Enter URL to export from");
        String url = scanner.nextLine();
        LanguageExport languageExport = getLanguageExportFromUrl(url);
        if (languageExport == null) {
            throw new IllegalStateException("An error occurred: constructed language export is null");
        }

        mergeExportIntoFile(languageExport);
        System.out.println("Saved to messages file for code '" + languageExport.code + "'");
    }

    private LanguageExport getLanguageExportFromUrl(String url) {
        try {
            URL uri = new URL(url);
            String json = Resources.toString(uri, Charset.forName("UTF-8"));
            return gson.fromJson(json, LanguageExport.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void mergeExportIntoFile(LanguageExport export) {
        String languageCode = export.code;
        File file = new File(MESSAGES_FOLDER + "messages_" + languageCode + ".yml");
        if (!file.exists()) {
            throw new IllegalStateException("Messages file for language code " + languageCode + " does not exist");
        }
        FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(file);

        for (MessageExport messageExport : export.messages) {
            fileConfiguration.set(messageExport.key, messageExport.translatedMessage);
        }
        try {
            Field dumperOptionsField = YamlConfiguration.class.getDeclaredField("yamlOptions");
            dumperOptionsField.setAccessible(true);
            DumperOptions options = (DumperOptions) dumperOptionsField.get(fileConfiguration);
            options.setDefaultScalarStyle(DumperOptions.ScalarStyle.SINGLE_QUOTED);
            fileConfiguration.save(file);
        } catch (IOException | NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
