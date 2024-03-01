package fr.xephi.authme.permission;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Test for {@link PermissionsSystemType}.
 */
class PermissionsSystemTypeTest {

    @Test
    void shouldHaveDefinedAndUniqueNames() {
        // given / when / then
        List<String> names = new ArrayList<>(PermissionsSystemType.values().length);
        List<String> pluginNames = new ArrayList<>(PermissionsSystemType.values().length);

        for (PermissionsSystemType system : PermissionsSystemType.values()) {
            assertThat("Display name for enum entry '" + system + "' is not null",
                system.getDisplayName(), not(nullValue()));
            assertThat("Plugin name for enum entry '" + system + "' is not null",
                system.getPluginName(), not(nullValue()));
            assertThat("Only one enum entry has display name '" + system.getDisplayName() + "'",
                names, not(hasItem(system.getDisplayName())));
            assertThat("Only one enum entry has plugin name '" + system.getPluginName() + "'",
                pluginNames, not(hasItem(system.getPluginName())));
            names.add(system.getDisplayName());
            pluginNames.add(system.getPluginName());
        }
    }

    @Test
    void shouldRecognizePermissionSystemType() {
        assertThat(PermissionsSystemType.isPermissionSystem("bogus"), equalTo(false));
        assertThat(PermissionsSystemType.isPermissionSystem("PermissionsEx"), equalTo(true));
    }

}
