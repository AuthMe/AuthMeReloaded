package fr.xephi.authme.converter;

import java.io.File;

import fr.xephi.authme.settings.CustomConfiguration;

/**
 */
public class RoyalAuthYamlReader extends CustomConfiguration {

    /**
     * Constructor for RoyalAuthYamlReader.
     * @param file File
     */
    public RoyalAuthYamlReader(File file) {
        super(file);
        load();
        save();
    }

    /**
     * Method getLastLogin.
    
     * @return long */
    public long getLastLogin() {
        return getLong("timestamps.quit");
    }

    /**
     * Method getHash.
    
     * @return String */
    public String getHash() {
        return getString("login.password");
    }
}
