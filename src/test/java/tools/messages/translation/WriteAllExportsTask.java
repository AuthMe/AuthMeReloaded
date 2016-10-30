package tools.messages.translation;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import tools.utils.FileIoUtils;
import tools.utils.ToolsConstants;

import java.io.File;
import java.util.Scanner;

import static tools.utils.FileIoUtils.listFilesOrThrow;

/**
 * Task which exports all messages to a local folder.
 */
public class WriteAllExportsTask extends ExportMessagesTask {

    private static final String OUTPUT_FOLDER = ToolsConstants.TOOLS_SOURCE_ROOT + "messages/translation/export/";

    @Override
    public String getTaskName() {
        return "writeAllExports";
    }

    @Override
    public void execute(Scanner scanner) {
        final File[] messageFiles = listFilesOrThrow(new File(MESSAGES_FOLDER));
        final FileConfiguration defaultMessages = loadDefaultMessages();
        for (File file : messageFiles) {
            String code = file.getName().substring("messages_".length(), file.getName().length() - ".yml".length());
            String json = convertToJson(code, defaultMessages, YamlConfiguration.loadConfiguration(file));
            FileIoUtils.writeToFile(OUTPUT_FOLDER + "messages_" + code + ".json", json);
        }
    }
}
