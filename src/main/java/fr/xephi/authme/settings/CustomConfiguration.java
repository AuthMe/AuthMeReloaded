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

public class CustomConfiguration extends YamlConfiguration {

    private File configFile;

    public CustomConfiguration(File file) {
        this.configFile = file;
        load();
    }

    public void load() {
        try {
            super.load(configFile);
        } catch (FileNotFoundException e) {
            ConsoleLogger.showError("Could not find " + configFile.getName() + ", creating new one...");
            reLoad();
        } catch (IOException e) {
            ConsoleLogger.showError("Could not load " + configFile.getName());
        } catch (InvalidConfigurationException e) {
            ConsoleLogger.showError(configFile.getName() + " is no valid configuration file");
        }
    }

    public boolean reLoad() {
        boolean out = true;
        if (!configFile.exists()) {
            out = loadResource(configFile);
        }
        if (out)
            load();
        return out;
    }

    public void save() {
        try {
            super.save(configFile);
        } catch (IOException ex) {
            ConsoleLogger.showError("Could not save config to " + configFile.getName());
        }
    }

    public File getConfigFile() {
        return configFile;
    }

    public boolean loadResource(File file) {
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
