package fr.xephi.authme.permission;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link PermissionsSystemType}.
 */
public class PermissionsSystemTypeTest {

    @Test
    public void shouldHaveDefinedAndUniqueNames() {
        // given / when / then
        List<String> names = new ArrayList<>(PermissionsSystemType.values().length);
        List<String> pluginNames = new ArrayList<>(PermissionsSystemType.values().length);

        for (PermissionsSystemType system : PermissionsSystemType.values()) {
            assertThat("Name for enum entry '" + system + "' is not null",
                system.getName(), not(nullValue()));
            assertThat("Plugin name for enum entry '" + system + "' is not null",
                system.getPluginName(), not(nullValue()));
            assertThat("Only one enum entry has name '" + system.getName() + "'",
                names, not(hasItem(system.getName())));
            assertThat("Only one enum entry has plugin name '" + system.getPluginName() + "'",
                pluginNames, not(hasItem(system.getPluginName())));
            names.add(system.getName());
            pluginNames.add(system.getPluginName());
        }
    }

    @Test
    public void shouldRecognizePermissionSystemType() {
        assertThat(PermissionsSystemType.isPermissionSystem("bogus"), equalTo(false));
        assertThat(PermissionsSystemType.isPermissionSystem("PermissionsBukkit"), equalTo(true));
    }

}
