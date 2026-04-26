package fr.xephi.authme.bungee;

import ch.jalu.configme.configurationdata.ConfigurationData;
import ch.jalu.configme.migration.PlainMigrationService;
import ch.jalu.configme.resource.PropertyReader;
import fr.xephi.authme.bungee.config.BungeeConfigProperties;

import java.security.SecureRandom;
import java.util.HexFormat;

class HmacSecretMigrationService extends PlainMigrationService {

    @Override
    protected boolean performMigrations(PropertyReader reader, ConfigurationData configurationData) {
        if (configurationData.getValue(BungeeConfigProperties.PROXY_SHARED_SECRET).isEmpty()) {
            configurationData.setValue(BungeeConfigProperties.PROXY_SHARED_SECRET, generateSecret());
            return MIGRATION_REQUIRED;
        }
        return NO_MIGRATION_NEEDED;
    }

    private static String generateSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
