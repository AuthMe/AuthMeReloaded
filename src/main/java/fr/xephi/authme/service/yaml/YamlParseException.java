package fr.xephi.authme.service.yaml;

import ch.jalu.configme.exception.ConfigMeException;

import static com.google.common.base.MoreObjects.firstNonNull;

/**
 * Exception when a YAML file could not be parsed.
 */
public class YamlParseException extends RuntimeException {

    private final String file;

    /**
     * Constructor.
     *
     * @param file the file a parsing exception occurred with
     * @param configMeException the caught exception from ConfigMe
     */
    public YamlParseException(String file, ConfigMeException configMeException) {
        super(firstNonNull(configMeException.getCause(), configMeException));
        this.file = file;
    }

    public String getFile() {
        return file;
    }
}
