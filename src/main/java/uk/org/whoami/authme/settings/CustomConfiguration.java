package uk.org.whoami.authme.settings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomConfiguration extends YamlConfiguration{

	private File configFile;

	public CustomConfiguration(File file)
	{
		this.configFile = file;

		load();
	}

	public void load()
	{
		try {
			super.load(configFile);
		} catch (FileNotFoundException e) {
			Logger.getLogger(JavaPlugin.class.getName()).log(Level.SEVERE, "Could not find " + configFile.getName() + ", creating new one...");
			reload();
		} catch (IOException e) {
			Logger.getLogger(JavaPlugin.class.getName()).log(Level.SEVERE, "Could not load " + configFile.getName(), e);
		} catch (InvalidConfigurationException e) {
			Logger.getLogger(JavaPlugin.class.getName()).log(Level.SEVERE, configFile.getName() + " is no valid configuration file", e);
		}
	}

	public boolean reload() {
		boolean out = true;
		if (!configFile.exists())
		{
			out = loadRessource(configFile);
		}
		if (out) load();
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
			InputStream fis = getClass().getResourceAsStream("/" + file.getName());
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(file);
				byte[] buf = new byte[1024];
				int i = 0;
				while ((i = fis.read(buf)) != -1) {
					fos.write(buf, 0, i);
				}
			} catch (Exception e) {
				Logger.getLogger(JavaPlugin.class.getName()).log(Level.SEVERE, "Failed to load config from JAR");
				out = false;
			} finally {
				try {
					if (fis != null) {
						fis.close();
					}
					if (fos != null) {
						fos.close();
					}
				} catch (Exception e) {                         
				}
			}
		}
		return out;
	}
}
