package fr.xephi.authme;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.security.crypts.HashedPassword;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.Objects;

/**
 * Custom matchers for AuthMe entities.
 */
@SuppressWarnings("checkstyle:JavadocMethod") // Justification: Javadoc would be huge because of the many parameters
public final class AuthMeMatchers {

    private AuthMeMatchers() {
    }

    public static Matcher<? super HashedPassword> equalToHash(String hash) {
        return equalToHash(new HashedPassword(hash));
    }

    public static Matcher<? super HashedPassword> equalToHash(String hash, String salt) {
        return equalToHash(new HashedPassword(hash, salt));
    }

    public static Matcher<? super HashedPassword> equalToHash(HashedPassword hash) {
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

    public static Matcher<? super PlayerAuth> hasAuthBasicData(String name, String realName,
                                                               String email, String lastIp) {
        return new TypeSafeMatcher<PlayerAuth>() {
            @Override
            public boolean matchesSafely(PlayerAuth item) {
                return Objects.equals(name, item.getNickname())
                    && Objects.equals(realName, item.getRealName())
                    && Objects.equals(email, item.getEmail())
                    && Objects.equals(lastIp, item.getLastIp());
            }

            @Override
            public void describeTo(Description description) {
                description.appendValue(String.format("PlayerAuth with name %s, realname %s, email %s, lastIp %s",
                    name, realName, email, lastIp));
            }

            @Override
            public void describeMismatchSafely(PlayerAuth item, Description description) {
                description.appendValue(String.format("PlayerAuth with name %s, realname %s, email %s, lastIp %s",
                    item.getNickname(), item.getRealName(), item.getEmail(), item.getLastIp()));
            }
        };
    }

    public static Matcher<? super PlayerAuth> hasRegistrationInfo(String registrationIp, long registrationDate) {
        return new TypeSafeMatcher<PlayerAuth>() {
            @Override
            public boolean matchesSafely(PlayerAuth item) {
                return Objects.equals(registrationIp, item.getRegistrationIp())
                    && Objects.equals(registrationDate, item.getRegistrationDate());
            }

            @Override
            public void describeTo(Description description) {
                description.appendValue(String.format("PlayerAuth with reg. IP %s and reg date %d",
                    registrationIp, registrationDate));
            }

            @Override
            public void describeMismatchSafely(PlayerAuth item, Description description) {
                description.appendValue(String.format("PlayerAuth with reg. IP %s and reg date %d",
                    item.getRegistrationIp(), item.getRegistrationDate()));
            }
        };
    }

    public static Matcher<? super PlayerAuth> hasAuthLocation(PlayerAuth auth) {
        return hasAuthLocation(auth.getQuitLocX(), auth.getQuitLocY(), auth.getQuitLocZ(), auth.getWorld(),
            auth.getYaw(), auth.getPitch());
    }

    public static Matcher<? super PlayerAuth> hasAuthLocation(double x, double y, double z,
                                                              String world, float yaw, float pitch) {
        return new TypeSafeMatcher<PlayerAuth>() {
            @Override
            public boolean matchesSafely(PlayerAuth item) {
                return Objects.equals(x, item.getQuitLocX())
                    && Objects.equals(y, item.getQuitLocY())
                    && Objects.equals(z, item.getQuitLocZ())
                    && Objects.equals(world, item.getWorld())
                    && Objects.equals(yaw, item.getYaw())
                    && Objects.equals(pitch, item.getPitch());
            }

            @Override
            public void describeTo(Description description) {
                description.appendValue(
                    String.format("PlayerAuth with quit location (x: %f, y: %f, z: %f, world: %s, yaw: %f, pitch: %f)",
                        x, y, z, world, yaw, pitch));
            }
        };
    }

    public static Matcher<String> stringWithLength(int length) {
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
