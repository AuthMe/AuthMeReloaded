package fr.xephi.authme.message.updater;

import ch.jalu.configme.properties.Property;
import ch.jalu.configme.resource.PropertyReader;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.util.FileUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Returns messages from the JAR's message files. Favors a local JAR (e.g. messages_nl.yml)
 * before falling back to the default language (messages_en.yml).
 */
public class JarMessageSource {

    private final PropertyReader localJarMessages;
    private final PropertyReader defaultJarMessages;

    /**
     * Constructor.
     *
     * @param localJarPath path to the messages file of the language the plugin is configured to use (may not exist)
     * @param defaultJarPath path to the default messages file in the JAR (must exist)
     */
    public JarMessageSource(String localJarPath, String defaultJarPath) {
        localJarMessages = localJarPath.equals(defaultJarPath) ? null : loadJarFile(localJarPath);
        defaultJarMessages = loadJarFile(defaultJarPath);

        if (defaultJarMessages == null) {
            throw new IllegalStateException("Default JAR file '" + defaultJarPath + "' could not be loaded");
        }
    }

    public String getMessageFromJar(Property<?> property) {
        String key = property.getPath();
        String message = getString(key, localJarMessages);
        return message == null ? getString(key, defaultJarMessages) : message;
    }

    private static String getString(String path, PropertyReader reader) {
        return reader == null ? null : reader.getTypedObject(path, String.class);
    }

    private static MessageMigraterPropertyReader loadJarFile(String jarPath) {
        try (InputStream stream = FileUtils.getResourceFromJar(jarPath)) {
            if (stream == null) {
                ConsoleLogger.debug("Could not load '" + jarPath + "' from JAR");
                return null;
            }
            return MessageMigraterPropertyReader.loadFromStream(stream);
        } catch (IOException e) {
            ConsoleLogger.logException("Exception while handling JAR path '" + jarPath + "'", e);
        }
        return null;
    }
}
