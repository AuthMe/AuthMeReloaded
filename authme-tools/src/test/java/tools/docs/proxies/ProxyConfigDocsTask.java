package tools.docs.proxies;

import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.SettingsManagerBuilder;
import fr.xephi.authme.bungee.config.BungeeConfigProperties;
import fr.xephi.authme.velocity.config.VelocityConfigProperties;
import tools.utils.AutoToolTask;
import tools.utils.FileIoUtils;
import tools.utils.TagValueHolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static tools.utils.ToolsConstants.DOCS_FOLDER;
import static tools.utils.ToolsConstants.TOOLS_SOURCE_ROOT;

/**
 * Generates reference proxy config files in the docs folder.
 */
public class ProxyConfigDocsTask implements AutoToolTask {

    private static final Path PROXIES_DOCS_ROOT = Paths.get(DOCS_FOLDER, "proxies");
    private static final Path PROXIES_TEMPLATE_ROOT = Paths.get(TOOLS_SOURCE_ROOT, "docs", "proxies");

    @Override
    public String getTaskName() {
        return "updateProxyConfigDocs";
    }

    @Override
    public void executeDefault() {
        writeConfig(BungeeConfigProperties.class,
            PROXIES_TEMPLATE_ROOT.resolve(Paths.get("bungee", "config.tpl.yml")),
            PROXIES_DOCS_ROOT.resolve(Paths.get("bungee", "config.yml")));
        writeConfig(VelocityConfigProperties.class,
            PROXIES_TEMPLATE_ROOT.resolve(Paths.get("velocity", "config.tpl.yml")),
            PROXIES_DOCS_ROOT.resolve(Paths.get("velocity", "config.yml")));
    }

    private static void writeConfig(Class<? extends SettingsHolder> propertiesClass, Path templateFile, Path outputFile) {
        try {
            Path temporaryConfig = Files.createTempFile("authme-proxy-config-", ".yml");
            try {
                SettingsManager settingsManager = SettingsManagerBuilder.withYamlFile(temporaryConfig.toFile())
                    .configurationData(propertiesClass)
                    .create();
                settingsManager.save();

                Files.createDirectories(outputFile.getParent());
                FileIoUtils.generateFileFromTemplate(templateFile.toString(), outputFile.toString(),
                    TagValueHolder.create().put("config", FileIoUtils.readFromFile(temporaryConfig)));
                System.out.println("Wrote to '" + outputFile + "'");
            } finally {
                Files.deleteIfExists(temporaryConfig);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not generate proxy config docs for '" + outputFile + "'", e);
        }
    }
}
