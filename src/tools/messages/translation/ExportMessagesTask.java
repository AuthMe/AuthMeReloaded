package messages.translation;

import com.google.gson.Gson;
import fr.xephi.authme.output.MessageKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import utils.FileUtils;
import utils.ToolTask;
import utils.ToolsConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Task to export all messages for translation purposes.
 */
public class ExportMessagesTask implements ToolTask {

    private final Gson gson = new Gson();

    @Override
    public String getTaskName() {
        return "exportMessages";
    }

    @Override
    public void execute(Scanner scanner) {
        FileConfiguration defaultMessages = YamlConfiguration.loadConfiguration(
            new File(ToolsConstants.MAIN_RESOURCES_ROOT + "messages/messages_en.yml"));

        File[] messageFiles = new File(ToolsConstants.MAIN_RESOURCES_ROOT + "messages").listFiles();
        if (messageFiles == null || messageFiles.length == 0) {
            throw new IllegalStateException("Could not read messages folder");
        }

        for (File file : messageFiles) {
            String code = file.getName().substring("messages_".length(), file.getName().length() - ".yml".length());
            exportLanguage(code, defaultMessages, YamlConfiguration.loadConfiguration(file));
        }
    }

    private void exportLanguage(String code, FileConfiguration defaultMessages, FileConfiguration messageFile) {
        List<MessageExport> list = new ArrayList<>();
        for (MessageKey key : MessageKey.values()) {
            list.add(new MessageExport(key.getKey(), key.getTags(), getString(key, defaultMessages),
                getString(key, messageFile)));
        }

        FileUtils.writeToFile(
            ToolsConstants.TOOLS_SOURCE_ROOT + "messages/translation/export/messages_" + code + ".json",
            gson.toJson(new LanguageExport(code, list)));
    }

    private static String getString(MessageKey key, FileConfiguration configuration) {
        return configuration.getString(key.getKey(), "");
    }

}
