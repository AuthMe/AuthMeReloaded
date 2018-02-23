package fr.xephi.authme.service.yaml;

import ch.jalu.configme.resource.YamlFileResource;
import org.yaml.snakeyaml.parser.ParserException;

import java.io.File;

/**
 * Creates {@link YamlFileResource} objects.
 */
public final class YamlFileResourceProvider {

    private YamlFileResourceProvider() {
    }

    /**
     * Creates a {@link YamlFileResource} instance for the given file. Wraps SnakeYAML's parse exception
     * into an AuthMe exception.
     *
     * @param file the file to load
     * @return the generated resource
     */
    public static YamlFileResource loadFromFile(File file) {
        try {
            return new YamlFileResource(file);
        } catch (ParserException e) {
            throw new YamlParseException(file.getPath(), e);
        }
    }
}
