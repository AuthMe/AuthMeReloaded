package fr.xephi.authme.initialization;

import fr.xephi.authme.settings.Settings;

/**
 * Interface for classes that keep a local copy of certain settings.
 *
 * @see fr.xephi.authme.command.executable.authme.ReloadCommand
 */
public interface SettingsDependent {

    /**
     * Performs a reload with the provided settings instance.
     *
     * @param settings the settings instance
     */
    void reload(Settings settings);
}
