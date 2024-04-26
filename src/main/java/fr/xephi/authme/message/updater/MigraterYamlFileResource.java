package fr.xephi.authme.message.updater;

import ch.jalu.configme.resource.PropertyReader;
import ch.jalu.configme.resource.YamlFileResource;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;

/**
 * Extension of {@link YamlFileResource} to fine-tune the export style.
 */
public class MigraterYamlFileResource extends YamlFileResource {

    private Yaml singleQuoteYaml;

    public MigraterYamlFileResource(File file) {
        super(file);
    }

    @Override
    public @NotNull PropertyReader createReader() {
        return MessageMigraterPropertyReader.loadFromFile(getFile());
    }

    @Override
    protected @NotNull Yaml createNewYaml() {
        if (singleQuoteYaml == null) {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setAllowUnicode(true);
            options.setDefaultScalarStyle(DumperOptions.ScalarStyle.SINGLE_QUOTED);
            // Overridden setting: don't split lines
            options.setSplitLines(false);
            singleQuoteYaml = new Yaml(options);
        }
        return singleQuoteYaml;
    }
}
