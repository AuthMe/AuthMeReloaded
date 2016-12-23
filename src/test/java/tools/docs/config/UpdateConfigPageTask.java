package tools.docs.config;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.resource.YamlFileResource;
import fr.xephi.authme.settings.properties.AuthMeSettingsRetriever;
import fr.xephi.authme.util.FileUtils;
import tools.utils.AutoToolTask;
import tools.utils.FileIoUtils;
import tools.utils.TagValueHolder;
import tools.utils.ToolsConstants;

import java.io.File;
import java.io.IOException;

/**
 * Task for updating the config docs page.
 */
public class UpdateConfigPageTask implements AutoToolTask {

    private static final String TEMPLATE_FILE = ToolsConstants.TOOLS_SOURCE_ROOT + "docs/config/config.tpl.md";
    private static final String OUTPUT_FILE = ToolsConstants.DOCS_FOLDER + "config.md";

    @Override
    public String getTaskName() {
        return "updateConfigPage";
    }

    @Override
    public void executeDefault() {
        File config = null;
        try {
            // Create empty temporary .yml file and save the config to it
            config = File.createTempFile("authme-config-", ".yml");
            SettingsManager settingsManager = new SettingsManager(
                new YamlFileResource(config), null, AuthMeSettingsRetriever.buildConfigurationData());
            settingsManager.save();

            // Get the contents and generate template file
            TagValueHolder tagValueHolder = TagValueHolder.create()
                .put("config", FileIoUtils.readFromFile(config.toPath()));
            FileIoUtils.generateFileFromTemplate(TEMPLATE_FILE, OUTPUT_FILE, tagValueHolder);
            System.out.println("Wrote to '" + OUTPUT_FILE + "'");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            FileUtils.delete(config);
        }
    }
}
