package fr.xephi.authme.util;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link ServerUtils}.
 */
public class ServerUtilsTest {

    @Test
    public void shouldReturnTrueForSpigotImplementation() {
        // Spigot is a provided dependency of the project, so the ClassLoader knows about it
        assertThat(ServerUtils.isSpigot(), equalTo(true));
    }

}
