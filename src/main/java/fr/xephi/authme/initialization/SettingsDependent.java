package fr.xephi.authme.initialization;

import fr.xephi.authme.settings.NewSetting;

/**
 * Interface for classes that keep a local copy of certain settings.
 */
public interface SettingsDependent {

    /**
     * Performs a reload with the provided settings instance.
     *
     * @param settings the settings instance
     */
    void reload(NewSetting settings);
}
