package fr.xephi.authme.velocity;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.SettingsManagerBuilder;
import fr.xephi.authme.velocity.config.VelocityConfigProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class VelocityConfigManager {

    private final SettingsManager settingsManager;

    VelocityConfigManager(Path dataDirectory) {
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create AuthMe Velocity data directory", e);
        }

        this.settingsManager = SettingsManagerBuilder.withYamlFile(dataDirectory.resolve("config.yml").toFile())
            .configurationData(VelocityConfigProperties.class)
            .migrationService(new HmacSecretMigrationService())
            .create();
    }


    VelocityProxyConfiguration getConfiguration() {
        return VelocityProxyConfiguration.from(settingsManager);
    }

    VelocityProxyConfiguration reload() {
        settingsManager.reload();
        return VelocityProxyConfiguration.from(settingsManager);
    }
}
