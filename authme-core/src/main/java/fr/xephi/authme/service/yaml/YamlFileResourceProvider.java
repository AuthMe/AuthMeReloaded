package fr.xephi.authme.service.yaml;

import ch.jalu.configme.exception.ConfigMeException;
import ch.jalu.configme.resource.PropertyReader;
import ch.jalu.configme.resource.YamlFileResource;

import java.io.File;

/**
 * Creates {@link YamlFileResource} objects.
 */
public final class YamlFileResourceProvider {

    private YamlFileResourceProvider() {
    }

    /**
     * Creates a {@link YamlFileResource} instance for the given file. Wraps SnakeYAML's parse exception
     * thrown when a reader is created into an AuthMe exception.
     *
     * @param file the file to load
     * @return the generated resource
     */
    public static YamlFileResource loadFromFile(File file) {
        return new AuthMeYamlFileResource(file);
    }

    /**
     * Extension of {@link YamlFileResource} which wraps SnakeYAML's parse exception into a custom
     * exception when a reader is created.
     */
    private static final class AuthMeYamlFileResource extends YamlFileResource {

        AuthMeYamlFileResource(File file) {
            super(file);
        }

        @Override
        public PropertyReader createReader() {
            try {
                return super.createReader();
            } catch (ConfigMeException e) {
                throw new YamlParseException(getFile().getPath(), e);
            }
        }
    }
}
