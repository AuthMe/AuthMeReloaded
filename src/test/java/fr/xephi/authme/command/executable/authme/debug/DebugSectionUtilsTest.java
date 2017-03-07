package fr.xephi.authme.command.executable.authme.debug;

import fr.xephi.authme.TestHelper;
import org.bukkit.Location;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link DebugSectionUtils}.
 */
public class DebugSectionUtilsTest {

    @Test
    public void shouldFormatLocation() {
        // given / when
        String result = DebugSectionUtils.formatLocation(0.0, 10.248592, -18934.2349023, "Main");

        // then
        assertThat(result, equalTo("(0, 10.25, -18934.23) in 'Main'"));
    }

    @Test
    public void shouldHandleNullWorld() {
        // given
        Location location = new Location(null, 3.7777, 2.14156, 1);

        // when
        String result = DebugSectionUtils.formatLocation(location);

        // then
        assertThat(result, equalTo("(3.78, 2.14, 1) in 'null'"));
    }

    @Test
    public void shouldHandleNullLocation() {
        // given / when / then
        assertThat(DebugSectionUtils.formatLocation(null), equalTo("null"));
    }

    @Test
    public void shouldHaveHiddenConstructor() {
        TestHelper.validateHasOnlyPrivateEmptyConstructor(DebugSectionUtils.class);
    }
}
