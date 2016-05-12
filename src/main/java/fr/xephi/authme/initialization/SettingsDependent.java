package fr.xephi.authme.initialization;

import fr.xephi.authme.settings.NewSetting;

/**
 * Interface for classes that keep a local copy of certain settings.
 */
public interface SettingsDependent {

    /**
     * Loads the needed settings.
     *
     * @param settings the settings instance
     */
    void loadSettings(NewSetting settings);
}
