package tools.messages;

import fr.xephi.authme.message.MessageKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import tools.utils.AutoToolTask;
import tools.utils.ToolsConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Task to add English messages as javadoc comment to each MessageKey enum entry.
 */
public class AddJavaDocToMessageEnumTask implements AutoToolTask {

    private static final String MESSAGES_FILE = ToolsConstants.MAIN_RESOURCES_ROOT + "messages/messages_en.yml";

    private FileConfiguration configuration;

    @Override
    public void executeDefault() {
        configuration = YamlConfiguration.loadConfiguration(new File(MESSAGES_FILE));

        List<String> entries = new ArrayList<>();
        for (MessageKey entry : MessageKey.values()) {
            String tags = entry.getTags().length == 0
                ? ""
                : ", \"" + String.join("\", \"", entry.getTags()) + "\"";

            entries.add("/** " + getMessageForJavaDoc(entry) + " */"
                + "\n\t" + entry.name() + "(\"" + entry.getKey() + "\"" + tags + ")");
        }

        System.out.println("\t" + String.join(",\n\n\t", entries) + ";");
    }

    @Override
    public String getTaskName() {
        return "addJavaDocToMessageEnum";
    }

    private String getMessageForJavaDoc(MessageKey key) {
        return configuration.getString(key.getKey())
            .replaceAll("&[0-9a-f]", "")
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }
}
