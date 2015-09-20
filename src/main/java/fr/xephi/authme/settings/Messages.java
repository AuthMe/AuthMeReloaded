package fr.xephi.authme.settings;

import fr.xephi.authme.ConsoleLogger;
import org.bukkit.command.CommandSender;
import java.io.File;

public class Messages extends CustomConfiguration {

    private static Messages singleton = null;
    private String lang = "en";

    public Messages(File file, String lang) {
        super(file);
        load();
        singleton = this;
        this.lang = lang;
    }

    public void send(CommandSender sender, String msg) {
        if (!Settings.messagesLanguage.equalsIgnoreCase(singleton.lang))
            singleton.reloadMessages();
        String loc = (String) singleton.get(msg);
        if (loc == null) {
            loc = "Error with Translation files, please contact the admin for verify or update translation";
            ConsoleLogger.showError("Error with the " + msg + " translation, verify in your " + getConfigFile() + " !");
        }
        for (String l : loc.split("&n")) {
            sender.sendMessage(l.replace("&", "\u00a7"));
        }
    }

    public String[] send(String msg) {
        if (!Settings.messagesLanguage.equalsIgnoreCase(singleton.lang)) {
            singleton.reloadMessages();
        }
        String s = (String) singleton.get(msg);
        if (s == null) {
            ConsoleLogger.showError("Error with the " + msg + " translation, verify in your " + getConfigFile() + " !");
            String[] loc = new String[1];
            loc[0] = "Error with " + msg + " translation; Please contact the admin for verify or update translation files";
            return (loc);
        }
        int i = s.split("&n").length;
        String[] loc = new String[i];
        int a;
        for (a = 0; a < i; a++) {
            loc[a] = ((String) this.get(msg)).split("&n")[a].replace("&", "\u00a7");
        }
        if (loc.length == 0) {
            loc[0] = "Error with " + msg + " translation; Please contact the admin for verify or update translation files";
        }
        return loc;
    }

    public static Messages getInstance() {
        if (singleton == null) {
            singleton = new Messages(Settings.messageFile, Settings.messagesLanguage);
        }
        return singleton;
    }

    public void reloadMessages() {
        singleton = new Messages(Settings.messageFile, Settings.messagesLanguage);
    }

}
