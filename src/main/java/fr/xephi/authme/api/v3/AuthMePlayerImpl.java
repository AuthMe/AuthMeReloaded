package fr.xephi.authme.api.v3;

import fr.xephi.authme.data.auth.PlayerAuth;

import java.time.Instant;
import java.util.UUID;

/**
 * Implementation of {@link AuthMePlayer}. This implementation is not part of the API and
 * may have breaking changes in subsequent releases.
 */
class AuthMePlayerImpl implements AuthMePlayer {

    private String name;
    private UUID uuid;
    private String email;

    private Instant registrationDate;
    private String registrationIpAddress;

    private Instant lastLoginDate;
    private String lastLoginIpAddress;

    AuthMePlayerImpl() {
    }

    /**
     * Maps the given player auth to an AuthMePlayer instance. Returns null if
     * the player auth is null.
     *
     * @param playerAuth the player auth or null
     * @return the mapped player auth, or null if the argument was null
     */
    static AuthMePlayer fromPlayerAuth(PlayerAuth playerAuth) {
        if (playerAuth == null) {
            return null;
        }

        AuthMePlayerImpl authMeUser = new AuthMePlayerImpl();
        authMeUser.name = playerAuth.getRealName();
        authMeUser.uuid = playerAuth.getUuid();
        authMeUser.email = nullIfDefault(playerAuth.getEmail(), PlayerAuth.DB_EMAIL_DEFAULT);
        Long lastLoginMillis = nullIfDefault(playerAuth.getLastLogin(), PlayerAuth.DB_LAST_LOGIN_DEFAULT);
        authMeUser.registrationDate = toInstant(playerAuth.getRegistrationDate());
        authMeUser.registrationIpAddress = playerAuth.getRegistrationIp();
        authMeUser.lastLoginDate = toInstant(lastLoginMillis);
        authMeUser.lastLoginIpAddress = nullIfDefault(playerAuth.getLastIp(), PlayerAuth.DB_LAST_IP_DEFAULT);
        return authMeUser;
    }

    @Override
    public String getName() {
        return name;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public Instant getRegistrationDate() {
        return registrationDate;
    }

    @Override
    public String getRegistrationIpAddress() {
        return registrationIpAddress;
    }

    @Override
    public Instant getLastLoginDate() {
        return lastLoginDate;
    }

    @Override
    public String getLastLoginIpAddress() {
        return lastLoginIpAddress;
    }

    private static Instant toInstant(Long epochMillis) {
        return epochMillis == null ? null : Instant.ofEpochMilli(epochMillis);
    }

    private static <T> T nullIfDefault(T value, T defaultValue) {
        return defaultValue.equals(value) ? null : value;
    }
}
