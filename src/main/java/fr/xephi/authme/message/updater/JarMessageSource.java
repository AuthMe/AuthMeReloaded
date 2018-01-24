package fr.xephi.authme.message.updater;

import ch.jalu.configme.properties.Property;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.util.FileUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Returns messages from the JAR's message files. Favors a local JAR (e.g. messages_nl.yml)
 * before falling back to the default language (messages_en.yml).
 */
public class JarMessageSource {

    private final FileConfiguration localJarConfiguration;
    private final FileConfiguration defaultJarConfiguration;

    /**
     * Constructor.
     *
     * @param localJarPath path to the messages file of the language the plugin is configured to use (may not exist)
     * @param defaultJarPath path to the default messages file in the JAR (must exist)
     */
    public JarMessageSource(String localJarPath, String defaultJarPath) {
        localJarConfiguration = localJarPath.equals(defaultJarPath) ? null : loadJarFile(localJarPath);
        defaultJarConfiguration = loadJarFile(defaultJarPath);

        if (defaultJarConfiguration == null) {
            throw new IllegalStateException("Default JAR file '" + defaultJarPath + "' could not be loaded");
        }
    }

    public String getMessageFromJar(Property<String> property) {
        String key = property.getPath();
        String message = localJarConfiguration == null ? null : localJarConfiguration.getString(key);
        return message == null ? defaultJarConfiguration.getString(key) : message;
    }

    private static YamlConfiguration loadJarFile(String jarPath) {
        try (InputStream stream = FileUtils.getResourceFromJar(jarPath)) {
            if (stream == null) {
                ConsoleLogger.debug("Could not load '" + jarPath + "' from JAR");
                return null;
            }
            try (InputStreamReader isr = new InputStreamReader(stream)) {
                return YamlConfiguration.loadConfiguration(isr);
            }
        } catch (IOException e) {
            ConsoleLogger.logException("Exception while handling JAR path '" + jarPath + "'", e);
        }
        return null;
    }
}
