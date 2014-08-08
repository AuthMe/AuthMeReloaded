package fr.xephi.authme.converter;

import java.io.File;

import fr.xephi.authme.settings.CustomConfiguration;

public class RoyalAuthYamlReader extends CustomConfiguration {

    public RoyalAuthYamlReader(File file) {
        super(file);
        load();
        save();
    }

    public long getLastLogin() {
        return getLong("timestamps.quit");
    }

    public String getHash() {
        return getString("login.password");
    }
}
