package fr.xephi.authme.api.v3;

import fr.xephi.authme.data.auth.PlayerAuth;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Test for {@link AuthMePlayerImpl}.
 */
class AuthMePlayerImplTest {

    @Test
    void shouldMapNullWithoutError() {
        // given / when / then
        assertThat(AuthMePlayerImpl.fromPlayerAuth(null), emptyOptional());
    }

    @Test
    void shouldMapFromPlayerAuth() {
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
        Optional<AuthMePlayer> result = AuthMePlayerImpl.fromPlayerAuth(auth);

        // then
        AuthMePlayer playerInfo = result.get();
        assertThat(playerInfo.getName(), equalTo("Victor"));
        assertThat(playerInfo.getUuid().get(), equalTo(auth.getUuid()));
        assertThat(playerInfo.getEmail().get(), equalTo(auth.getEmail()));
        assertThat(playerInfo.getRegistrationDate(), equalTo(Instant.ofEpochMilli(auth.getRegistrationDate())));
        assertThat(playerInfo.getRegistrationIpAddress().get(), equalTo(auth.getRegistrationIp()));
        assertThat(playerInfo.getLastLoginDate().get(), equalTo(Instant.ofEpochMilli(auth.getLastLogin())));
        assertThat(playerInfo.getLastLoginIpAddress().get(), equalTo(auth.getLastIp()));
    }

    @Test
    void shouldHandleNullAndDefaultValues() {
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
        Optional<AuthMePlayer> result = AuthMePlayerImpl.fromPlayerAuth(auth);

        // then
        AuthMePlayer playerInfo = result.get();
        assertThat(playerInfo.getName(), equalTo("Victor"));
        assertThat(playerInfo.getUuid(), emptyOptional());
        assertThat(playerInfo.getEmail(), emptyOptional());
        assertThat(playerInfo.getRegistrationDate(), equalTo(Instant.ofEpochMilli(auth.getRegistrationDate())));
        assertThat(playerInfo.getRegistrationIpAddress(), emptyOptional());
        assertThat(playerInfo.getLastLoginDate(), emptyOptional());
        assertThat(playerInfo.getLastLoginIpAddress(), emptyOptional());
    }

    private static <T> Matcher<Optional<T>> emptyOptional() {
        return new TypeSafeMatcher<Optional<T>>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("an empty optional");
            }

            @Override
            protected boolean matchesSafely(Optional<T> item) {
                return !item.isPresent();
            }
        };
    }
}
