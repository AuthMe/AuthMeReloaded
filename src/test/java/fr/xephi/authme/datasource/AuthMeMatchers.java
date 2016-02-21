package fr.xephi.authme.datasource;

import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.security.crypts.HashedPassword;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.util.Objects;

/**
 * Custom matchers for AuthMe entities.
 */
public final class AuthMeMatchers {

    private AuthMeMatchers() {
    }

    public static Matcher<? super HashedPassword> equalToHash(final String hash) {
        return equalToHash(hash, null);
    }

    public static Matcher<? super HashedPassword> equalToHash(final String hash, final String salt) {
        return new BaseMatcher<HashedPassword>() {
            @Override
            public boolean matches(Object item) {
                if (item instanceof HashedPassword) {
                    HashedPassword input = (HashedPassword) item;
                    return Objects.equals(hash, input.getHash()) && Objects.equals(salt, input.getSalt());
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                String representation = "'" + hash + "'";
                if (salt != null) {
                    representation += ", '" + salt + "'";
                }
                description.appendValue("HashedPassword(" + representation + ")");
            }
        };
    }

    public static Matcher<? super PlayerAuth> hasAuthBasicData(final String name, final String realName,
                                                               final String email, final String ip) {
        return new BaseMatcher<PlayerAuth>() {
            @Override
            public boolean matches(Object item) {
                if (item instanceof PlayerAuth) {
                    PlayerAuth input = (PlayerAuth) item;
                    return Objects.equals(name, input.getNickname())
                        && Objects.equals(realName, input.getRealName())
                        && Objects.equals(email, input.getEmail())
                        && Objects.equals(ip, input.getIp());
                }
                return false;
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
        return new BaseMatcher<PlayerAuth>() {
            @Override
            public boolean matches(Object item) {
                if (item instanceof PlayerAuth) {
                    PlayerAuth input = (PlayerAuth) item;
                    return Objects.equals(x, input.getQuitLocX())
                        && Objects.equals(y, input.getQuitLocY())
                        && Objects.equals(z, input.getQuitLocZ())
                        && Objects.equals(world, input.getWorld());
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendValue(String.format("PlayerAuth with quit location (x: %f, y: %f, z: %f, world: %s)",
                    x, y, z, world));
            }
        };
    }

}
