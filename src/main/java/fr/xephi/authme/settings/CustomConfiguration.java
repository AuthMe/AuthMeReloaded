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
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

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
            Logger.getLogger(JavaPlugin.class.getName()).log(Level.SEVERE, "Could not find " + configFile.getName() + ", creating new one...");
            reLoad();
        } catch (IOException e) {
            Logger.getLogger(JavaPlugin.class.getName()).log(Level.SEVERE, "Could not load " + configFile.getName(), e);
        } catch (InvalidConfigurationException e) {
            Logger.getLogger(JavaPlugin.class.getName()).log(Level.SEVERE, configFile.getName() + " is no valid configuration file", e);
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
            Logger.getLogger(JavaPlugin.class.getName()).log(Level.SEVERE, "Could not save config to " + configFile.getName(), ex);
        }
    }

    public boolean loadRessource(File file) {
        boolean out = true;
        if (!file.exists()) {
            try {
                InputStream fis = getClass().getResourceAsStream("/" + file.getName());
                BufferedReader reader = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8").newDecoder()));
                String str;
                Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8").newEncoder()));
                while ((str = reader.readLine()) != null) {
                    writer.append(str).append("\r\n");
                }
                writer.flush();
                writer.close();
                reader.close();
                fis.close();
            } catch (Exception e) {
                Logger.getLogger(JavaPlugin.class.getName()).log(Level.SEVERE, "Failed to load config from JAR");
                out = false;
            }
            /*
             * FileOutputStream fos = null; try { fos = new
             * FileOutputStream(file); byte[] buf = new byte[1024]; int i = 0;
             * while ((i = fis.read(buf)) != -1) { fos.write(buf, 0, i); } }
             * catch (Exception e) {
             * Logger.getLogger(JavaPlugin.class.getName()).log(Level.SEVERE,
             * "Failed to load config from JAR"); out = false; } finally { try {
             * if (fis != null) { fis.close(); } if (fos != null) { fos.close();
             * } } catch (Exception e) { } } }
             */
        }
        return out;
    }
}
