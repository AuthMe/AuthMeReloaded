package fr.xephi.authme.settings;

import java.io.File;
import java.io.InputStream;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;

public class Messages extends CustomConfiguration {

    private static Messages singleton = null;

    public Messages(File file) {
        super(file);
        loadDefaults(file);
        loadFile();
        saveDefaults(file);
        singleton = this;
    }

    /**
     * Loads a file from the plugin jar and sets as default
     *
     * @param filename
     *            The filename to open
     */
    public final void loadDefaults(File file) {
        InputStream stream = AuthMe.getInstance().getResource(file.getName());
        if (stream == null)
            return;

        setDefaults(YamlConfiguration.loadConfiguration(stream));
    }

    /**
     * Saves the configuration to disk
     *
     * @return True if saved successfully
     */
    public final boolean saved(File file) {
        try {
            save(file);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Saves current configuration (plus defaults) to disk.
     *
     * If defaults and configuration are empty, saves blank file.
     *
     * @return True if saved successfully
     */
    public final boolean saveDefaults(File file) {
        options().copyDefaults(true);
        options().copyHeader(true);
        boolean success = saved(file);
        options().copyDefaults(false);
        options().copyHeader(false);
        return success;
    }

    private void loadFile() {
        this.load();
        this.save();
    }

    public void _(CommandSender sender, String msg) {
        String loc = (String) this.get(msg);
        if (loc == null) {
            loc = "Error with Translation files; Please contact the admin for verify or update translation";
            ConsoleLogger.showError("Error with the " + msg + " translation, verify in your " + Settings.MESSAGE_FILE + "_" + Settings.messagesLanguage + ".yml !");
        }
        for (String l : loc.split("&n")) {
            sender.sendMessage(l.replace("&", "\u00a7"));
        }
    }

    public String[] _(String msg) {
        int i = ((String) this.get(msg)).split("&n").length;
        String[] loc = new String[i];
        int a;
        for (a = 0; a < i; a++) {
            loc[a] = ((String) this.get(msg)).split("&n")[a].replace("&", "\u00a7");
        }
        if (loc == null || loc.length == 0) {
            loc[0] = "Error with " + msg + " translation; Please contact the admin for verify or update translation files";
            ConsoleLogger.showError("Error with the " + msg + " translation, verify in your " + Settings.MESSAGE_FILE + "_" + Settings.messagesLanguage + ".yml !");
        }
        return loc;
    }

    public static Messages getInstance() {
        if (singleton == null) {
            singleton = new Messages(new File(Settings.MESSAGE_FILE + "_" + Settings.messagesLanguage + ".yml"));
        }
        return singleton;
    }

}
