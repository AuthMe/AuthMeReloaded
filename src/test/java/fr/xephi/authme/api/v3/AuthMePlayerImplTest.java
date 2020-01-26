package fr.xephi.authme.api.v3;

import fr.xephi.authme.data.auth.PlayerAuth;
import org.junit.Test;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * Test for {@link AuthMePlayerImpl}.
 */
public class AuthMePlayerImplTest {

    @Test
    public void shouldMapNullWithoutError() {
        // given / when / then
        assertThat(AuthMePlayerImpl.fromPlayerAuth(null), nullValue());
    }

    @Test
    public void shouldMapFromPlayerAuth() {
        // given
        PlayerAuth auth = PlayerAuth.builder()
            .name("victor")
            .realName("Victor")
            .email("vic@example.com")
            .registrationDate(1480075661000L)
            .registrationIp("124.125.126.127")
            .lastLogin(1542675632000L)
            .lastIp("62.63.64.65")
            .uuid(UUID.fromString("deadbeef-2417-4653-9026-feedbabeface"))
            .build();

        // when
        AuthMePlayer result = AuthMePlayerImpl.fromPlayerAuth(auth);

        // then
        assertThat(result.getName(), equalTo("Victor"));
        assertThat(result.getUuid(), equalTo(auth.getUuid()));
        assertThat(result.getEmail(), equalTo(auth.getEmail()));
        assertThat(result.getRegistrationDate(), equalTo(Instant.ofEpochMilli(auth.getRegistrationDate())));
        assertThat(result.getRegistrationIpAddress(), equalTo(auth.getRegistrationIp()));
        assertThat(result.getLastLoginDate(), equalTo(Instant.ofEpochMilli(auth.getLastLogin())));
        assertThat(result.getLastLoginIpAddress(), equalTo(auth.getLastIp()));
    }

    @Test
    public void shouldHandleNullAndDefaultValues() {
        // given
        PlayerAuth auth = PlayerAuth.builder()
            .name("victor")
            .realName("Victor")
            .email("your@email.com") // DB default
            .registrationDate(1480075661000L)
            .lastLogin(0L) // DB default
            .lastIp("127.0.0.1") // DB default
            .build();

        // when
        AuthMePlayer result = AuthMePlayerImpl.fromPlayerAuth(auth);

        // then
        assertThat(result.getName(), equalTo("Victor"));
        assertThat(result.getUuid(), nullValue());
        assertThat(result.getEmail(), nullValue());
        assertThat(result.getRegistrationDate(), equalTo(Instant.ofEpochMilli(auth.getRegistrationDate())));
        assertThat(result.getRegistrationIpAddress(), nullValue());
        assertThat(result.getLastLoginDate(), nullValue());
        assertThat(result.getLastLoginIpAddress(), nullValue());
    }
}
