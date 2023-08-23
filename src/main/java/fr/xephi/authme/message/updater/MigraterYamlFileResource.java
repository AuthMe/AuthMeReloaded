package fr.xephi.authme.message.updater;

import ch.jalu.configme.resource.PropertyReader;
import ch.jalu.configme.resource.YamlFileResource;
import ch.jalu.configme.resource.yaml.SnakeYamlNodeBuilder;
import ch.jalu.configme.resource.yaml.SnakeYamlNodeBuilderImpl;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.File;

/**
 * Extension of {@link YamlFileResource} to fine-tune the export style.
 */
public class MigraterYamlFileResource extends YamlFileResource {

    public MigraterYamlFileResource(File file) {
        super(file);
    }

    @Override
    public PropertyReader createReader() {
        return MessageMigraterPropertyReader.loadFromFile(getPath());
    }

    @Override
    protected Yaml createNewYaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setAllowUnicode(true);
        options.setProcessComments(true);
        options.setIndent(4);
        // Overridden setting: don't split lines
        options.setSplitLines(false);
        return new Yaml(options);
    }

    @Override
    protected @NotNull SnakeYamlNodeBuilder createNodeBuilder() {
        return new MigraterYamlNodeBuilder();
    }

    /** Extended to represent all strings with single quotes in the YAML. */
    private static final class MigraterYamlNodeBuilder extends SnakeYamlNodeBuilderImpl {

        @Override
        protected @NotNull Node createStringNode(@NotNull String value) {
            return new ScalarNode(Tag.STR, value, null, null, DumperOptions.ScalarStyle.SINGLE_QUOTED);
        }

        @Override
        public @NotNull Node createKeyNode(@NotNull String key) {
            return super.createStringNode(key); // no single quotes
        }
    }
}
