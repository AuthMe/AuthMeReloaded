package fr.xephi.authme.settings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import fr.xephi.authme.ConsoleLogger;

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
            out = loadRessource(configFile);
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

    public boolean loadRessource(File file) {
        boolean out = true;
        if (!file.exists()) {
            try {
                String charset = System.getProperty("file.encoding");
                String newline = System.getProperty("line.separator");
                InputStream fis = getClass().getResourceAsStream("/" + file.getName());
                BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
                String str;
                Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), charset));
                while ((str = reader.readLine()) != null) {
                    writer.append(str).append(newline);
                }
                writer.flush();
                writer.close();
                reader.close();
                fis.close();
            } catch (Exception e) {
                ConsoleLogger.showError("Failed to load config from JAR");
                out = false;
            }
        }
        return out;
    }
}
