package fr.xephi.authme.message.updater;

import ch.jalu.configme.resource.PropertyReader;
import ch.jalu.configme.resource.YamlFileResource;
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
    public PropertyReader createReader() {
        return MessageMigraterPropertyReader.loadFromFile(getFile());
    }

    @Override
    protected Yaml createNewYaml() {
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

    // Because we set the YAML object to put strings in single quotes, this method by default uses that YAML object
    // and also puts all paths as single quotes. Override to just always return the same string since we know those
    // are only message names (so never any conflicting strings like "true" or "0").
    @Override
    protected String escapePathElementIfNeeded(String path) {
        return path;
    }
}
