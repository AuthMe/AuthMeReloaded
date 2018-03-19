package fr.xephi.authme.message.updater;

import ch.jalu.configme.beanmapper.leafproperties.LeafPropertiesGenerator;
import ch.jalu.configme.configurationdata.ConfigurationData;
import ch.jalu.configme.exception.ConfigMeException;
import ch.jalu.configme.properties.Property;
import ch.jalu.configme.resource.PropertyPathTraverser;
import ch.jalu.configme.resource.YamlFileResource;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import static fr.xephi.authme.message.updater.MessageMigraterPropertyReader.CHARSET;

/**
 * Extension of {@link YamlFileResource} to fine-tune the export style
 * and to be able to specify the character encoding.
 */
public class MigraterYamlFileResource extends YamlFileResource {

    private static final String INDENTATION = "    ";

    private final File file;
    private Yaml singleQuoteYaml;

    public MigraterYamlFileResource(File file) {
        super(file, MessageMigraterPropertyReader.loadFromFile(file), new LeafPropertiesGenerator());
        this.file = file;
    }

    @Override
    protected Yaml getSingleQuoteYaml() {
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

    @Override
    public void exportProperties(ConfigurationData configurationData) {
        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter writer = new OutputStreamWriter(fos, CHARSET)) {
            PropertyPathTraverser pathTraverser = new PropertyPathTraverser(configurationData);
            for (Property<?> property : convertPropertiesToExportableTypes(configurationData.getProperties())) {

                List<PropertyPathTraverser.PathElement> pathElements = pathTraverser.getPathElements(property);
                for (PropertyPathTraverser.PathElement pathElement : pathElements) {
                    writeComments(writer, pathElement.indentationLevel, pathElement.comments);
                    writer.append("\n")
                        .append(indent(pathElement.indentationLevel))
                        .append(pathElement.name)
                        .append(":");
                }

                writer.append(" ")
                    .append(toYaml(property, pathElements.get(pathElements.size() - 1).indentationLevel));
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new ConfigMeException("Could not save config to '" + file.getPath() + "'", e);
        } finally {
            singleQuoteYaml = null;
        }
    }

    private void writeComments(Writer writer, int indentation, String[] comments) throws IOException {
        if (comments.length == 0) {
            return;
        }
        String commentStart = "\n" + indent(indentation) + "# ";
        for (String comment : comments) {
            writer.append(commentStart).append(comment);
        }
    }

    private <T> String toYaml(Property<T> property, int indent) {
        Object value = property.getValue(this);
        String representation = transformValue(property, value);
        String[] lines = representation.split("\\n");
        return String.join("\n" + indent(indent), lines);
    }

    private static String indent(int level) {
        String result = "";
        for (int i = 0; i < level; i++) {
            result += INDENTATION;
        }
        return result;
    }
}
