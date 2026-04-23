package fr.xephi.authme.bungee;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.SettingsManagerBuilder;
import fr.xephi.authme.bungee.config.BungeeConfigProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class BungeeConfigManager {

    private final SettingsManager settingsManager;
    private final BungeeProxyConfiguration configuration;

    BungeeConfigManager(Path dataDirectory) {
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create AuthMe Bungee data directory", e);
        }

        this.settingsManager = SettingsManagerBuilder.withYamlFile(dataDirectory.resolve("config.yml").toFile())
            .configurationData(BungeeConfigProperties.class)
            .migrationService(new HmacSecretMigrationService())
            .create();
        this.configuration = BungeeProxyConfiguration.from(settingsManager);
    }

    BungeeProxyConfiguration getConfiguration() {
        return configuration;
    }

    BungeeProxyConfiguration reload() {
        settingsManager.reload();
        return BungeeProxyConfiguration.from(settingsManager);
    }
}
