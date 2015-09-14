package fr.xephi.authme.settings;

import com.google.common.io.Files;
import com.google.common.io.Resources;
import fr.xephi.authme.ConsoleLogger;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

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

    public boolean loadResource(File file) {
        if (!file.exists()) {
            try {
                if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                    return false;
                }
                if (!file.exists() && !file.createNewFile()) {
                    return false;
                }
                int i = file.getPath().indexOf("AuthMe");
                if (i > -1) {
                    String path = file.getPath().substring(i + 6).replace('\\', '/');
                    URL url = Resources.getResource(getClass(), path);
                    byte[] bytes = Resources.toByteArray(url);
                    Files.write(bytes, file);
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
