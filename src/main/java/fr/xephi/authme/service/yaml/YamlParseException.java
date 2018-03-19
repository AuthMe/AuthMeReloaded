package fr.xephi.authme.service.yaml;

import org.yaml.snakeyaml.parser.ParserException;

/**
 * Exception when a YAML file could not be parsed.
 */
public class YamlParseException extends RuntimeException {

    private final String file;

    /**
     * Constructor.
     *
     * @param file the file a parsing exception occurred with
     * @param snakeYamlException the caught exception from SnakeYAML
     */
    public YamlParseException(String file, ParserException snakeYamlException) {
        super(snakeYamlException);
        this.file = file;
    }

    public String getFile() {
        return file;
    }
}
