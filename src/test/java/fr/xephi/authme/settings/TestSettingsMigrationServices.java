package fr.xephi.authme.settings;

import com.github.authme.configme.migration.MigrationService;
import com.github.authme.configme.propertymap.PropertyEntry;
import com.github.authme.configme.resource.PropertyResource;

import java.util.List;

/**
 * Provides {@link MigrationService} implementations for testing.
 */
public final class TestSettingsMigrationServices {

    private TestSettingsMigrationServices() {
    }

    /**
     * Returns a settings migration service which always answers that all data is up-to-date.
     *
     * @return test settings migration service
     */
    public static MigrationService alwaysFulfilled() {
        return new MigrationService() {
            @Override
            public boolean checkAndMigrate(PropertyResource propertyResource, List<PropertyEntry> list) {
                return false;
            }
        };
    }
}
