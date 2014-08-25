package fr.xephi.authme.settings;

import java.io.File;
import java.util.ArrayList;

public class SpoutCfg extends CustomConfiguration {

    private static SpoutCfg instance = null;

    public SpoutCfg(File file) {
        super(file);
        loadDefaults();
        load();
        save();
    }

    @SuppressWarnings("serial")
    private void loadDefaults() {
        this.set("Spout GUI enabled", true);
        this.set("LoginScreen.enabled", true);
        this.set("LoginScreen.exit button", "Quit");
        this.set("LoginScreen.exit message", "Good Bye");
        this.set("LoginScreen.login button", "Login");
        this.set("LoginScreen.title", "LOGIN");
        this.set("LoginScreen.text", new ArrayList<String>() {

            {
                add("Sample text");
                add("Change this at spout.yml");
                add("--- AuthMe Reloaded by ---");
                add("Xephi59");
            }
        });
    }

    public static SpoutCfg getInstance() {
        if (instance == null)
            instance = new SpoutCfg(new File("plugins" + File.separator + "AuthMe", "spout.yml"));
        return instance;
    }
}
