package fr.xephi.authme.settings;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public abstract class CustomConfiguration extends YamlConfiguration {

    /**
     * The file of the configuration.
     */
    private final File configFile;

    /**
     * Constructor.
     * This loads the configuration file.
     *
     * @param file The file of the configuration.
     */
    public CustomConfiguration(File file) {
        this(file, true);
    }

    /**
     * Constructor.
     *
     * @param file The file of the configuration.
     * @param load True to load the configuration file.
     */
    public CustomConfiguration(File file, boolean load) {
        // Set the configuration file
        this.configFile = file;

        // Load the configuration file
        if(load)
            load();
    }

    /**
     * Load the configuration.
     */
    public void load() {
        // Try to load the configuration, catch exceptions
        try {
            // Load the configuration
            super.load(configFile);

        } catch (FileNotFoundException e) {
            // Show an error message
            ConsoleLogger.showError("Could not find " + configFile.getName() + ", creating new one...");

            // Reload the configuration and create a new file
            reload();

        } catch (IOException e) {
            // Show an error message
            ConsoleLogger.showError("Could not load " + configFile.getName());

        } catch (InvalidConfigurationException e) {
            // Show an error message
            ConsoleLogger.showError(configFile.getName() + " is no valid configuration file");
        }
    }

    /**
     * Reload the configuration.
     *
     * @return
     */
    public boolean reload() {
        boolean out = true;
        if (!configFile.exists()) {
            out = loadResource(configFile);
        }
        if (out)
            load();
        return out;
    }

    /**
     * Save the configuration.
     */
    public void save() {
        // Try to save the configuration, catch exceptions
        try {
            // Save the configuration
            super.save(configFile);

        } catch (IOException ex) {
            // Show an error message
            ConsoleLogger.showError("Could not save config to " + configFile.getName());
        }
    }

    /**
     * Get the configuration file.
     *
     * @return File.
     */

    public File getConfigFile() {
        return configFile;
    }

    private boolean loadResource(File file) {
        if (!file.exists()) {
            try {
                if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                    return false;
                }
                int i = file.getPath().indexOf("AuthMe");
                if (i > -1) {
                    String path = file.getPath().substring(i + 6).replace('\\', '/');
                    InputStream is = AuthMe.class.getResourceAsStream(path);
                    Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    return true;
                }
            } catch (Exception e) {
                ConsoleLogger.writeStackTrace(e);
                ConsoleLogger.showError("Failed to load config from JAR");
            }
        }
        return false;
    }
}
