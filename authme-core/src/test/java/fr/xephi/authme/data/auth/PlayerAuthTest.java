package fr.xephi.authme.data.auth;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test for {@link PlayerAuth} and its builder.
 */
class PlayerAuthTest {

    @Test
    void shouldRemoveDatabaseDefaults() {
        // given / when
        PlayerAuth auth = PlayerAuth.builder()
            .name("Bobby")
            .lastLogin(0L)
            .lastIp("127.0.0.1")
            .email("your@email.com")
            .build();

        // then
        assertThat(auth.getNickname(), equalTo("bobby"));
        assertThat(auth.getLastLogin(), nullValue());
        // Note ljacqu 20171020: Although 127.0.0.1 is the default value, we need to keep it because it might
        // legitimately be the resolved IP of a player
        assertThat(auth.getLastIp(), equalTo("127.0.0.1"));
        assertThat(auth.getEmail(), nullValue());
    }

    @Test
    void shouldThrowForMissingName() {
        try {
            // given / when
            PlayerAuth.builder()
                .email("test@example.org")
                .groupId(3)
                .build();

            // then
            fail("Expected exception to be thrown");
        } catch (NullPointerException e) {
            // all good
        }
    }

    @Test
    void shouldCreatePlayerAuthWithNullValues() {
        // given / when
        PlayerAuth auth = PlayerAuth.builder()
            .name("Charlie")
            .email(null)
            .lastLogin(null)
            .lastIp(null)
            .groupId(19)
            .locPitch(123.004f)
            .build();

        // then
        assertThat(auth.getEmail(), nullValue());
        assertThat(auth.getLastLogin(), nullValue());
        assertThat(auth.getLastIp(), nullValue());
        assertThat(auth.getGroupId(), equalTo(19));
        assertThat(auth.getPitch(), equalTo(123.004f));
    }
}
