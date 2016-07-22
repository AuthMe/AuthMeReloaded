package fr.xephi.authme;

import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.security.crypts.HashedPassword;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.Objects;

/**
 * Custom matchers for AuthMe entities.
 */
public final class AuthMeMatchers {

    private AuthMeMatchers() {
    }

    public static Matcher<? super HashedPassword> equalToHash(final String hash) {
        return equalToHash(new HashedPassword(hash));
    }

    public static Matcher<? super HashedPassword> equalToHash(final String hash, final String salt) {
        return equalToHash(new HashedPassword(hash, salt));
    }

    public static Matcher<? super HashedPassword> equalToHash(final HashedPassword hash) {
        return new TypeSafeMatcher<HashedPassword>() {
            @Override
            public boolean matchesSafely(HashedPassword item) {
                return Objects.equals(hash.getHash(), item.getHash())
                    && Objects.equals(hash.getSalt(), item.getSalt());
            }

            @Override
            public void describeTo(Description description) {
                String representation = "'" + hash.getHash() + "'";
                if (hash.getSalt() != null) {
                    representation += ", '" + hash.getSalt() + "'";
                }
                description.appendValue("HashedPassword(" + representation + ")");
            }
        };
    }

    public static Matcher<? super PlayerAuth> hasAuthBasicData(final String name, final String realName,
                                                               final String email, final String ip) {
        return new TypeSafeMatcher<PlayerAuth>() {
            @Override
            public boolean matchesSafely(PlayerAuth item) {
                return Objects.equals(name, item.getNickname())
                    && Objects.equals(realName, item.getRealName())
                    && Objects.equals(email, item.getEmail())
                    && Objects.equals(ip, item.getIp());
            }

            @Override
            public void describeTo(Description description) {
                description.appendValue(String.format("PlayerAuth with name %s, realname %s, email %s, ip %s",
                    name, realName, email, ip));
            }
        };
    }

    public static Matcher<? super PlayerAuth> hasAuthLocation(final double x, final double y, final double z,
                                                              final String world) {
        return new TypeSafeMatcher<PlayerAuth>() {
            @Override
            public boolean matchesSafely(PlayerAuth item) {
                return Objects.equals(x, item.getQuitLocX())
                    && Objects.equals(y, item.getQuitLocY())
                    && Objects.equals(z, item.getQuitLocZ())
                    && Objects.equals(world, item.getWorld());
            }

            @Override
            public void describeTo(Description description) {
                description.appendValue(String.format("PlayerAuth with quit location (x: %f, y: %f, z: %f, world: %s)",
                    x, y, z, world));
            }
        };
    }

    public static Matcher<String> stringWithLength(final int length) {
        return new TypeSafeMatcher<String>() {
            @Override
            protected boolean matchesSafely(String item) {
                return item != null && item.length() == length;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("String with length " + length);
            }
        };
    }

}
