package fr.xephi.authme.data;

import fr.xephi.authme.initialization.HasCleanup;
import fr.xephi.authme.util.expiring.ExpiringMap;

import javax.inject.Inject;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ProxySessionManager implements HasCleanup {

    private final ExpiringMap<String, ProxyLoginRequest> activeProxySessions;

    @Inject
    public ProxySessionManager() {
        long countTimeout = 15;
        activeProxySessions = new ExpiringMap<>(countTimeout, TimeUnit.SECONDS);
    }

    /**
     * Stores an auto-login request coming from the proxy.
     *
     * @param name the player's name
     * @param verifiedPremiumUuid the Mojang UUID cryptographically verified by the proxy, or null
     */
    public void processProxySessionMessage(String name, UUID verifiedPremiumUuid) {
        String normalizedName = name.toLowerCase(Locale.ROOT);
        activeProxySessions.put(normalizedName, new ProxyLoginRequest(normalizedName, verifiedPremiumUuid));
    }

    /**
     * Process a proxy session message from AuthMeBungee
     *
     * @param name the player to process
     */
    public void processProxySessionMessage(String name) {
        processProxySessionMessage(name, null);
    }

    /**
     * Returns if the player should be logged in or not
     *
     * @param name the name of the player to check
     * @return true if player has to be logged in, false otherwise
     */
    public boolean shouldResumeSession(String name) {
        return getLoginRequest(name) != null;
    }

    /**
     * Returns the current auto-login request for the player, or null if none is queued.
     *
     * @param name the player name
     * @return the queued proxy login request, or null
     */
    public ProxyLoginRequest getLoginRequest(String name) {
        return activeProxySessions.get(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Removes and returns the current auto-login request for the player.
     *
     * @param name the player name
     * @return the queued proxy login request, or null
     */
    public ProxyLoginRequest consumeLoginRequest(String name) {
        String normalizedName = name.toLowerCase(Locale.ROOT);
        ProxyLoginRequest loginRequest = activeProxySessions.get(normalizedName);
        if (loginRequest != null) {
            activeProxySessions.remove(normalizedName);
        }
        return loginRequest;
    }

    /**
     * Removes the queued auto-login request for the given player.
     *
     * @param name the player name
     */
    public void removeLoginRequest(String name) {
        activeProxySessions.remove(name.toLowerCase(Locale.ROOT));
    }

    @Override
    public void performCleanup() {
        activeProxySessions.removeExpiredEntries();
    }

    public record ProxyLoginRequest(String playerName, UUID verifiedPremiumUuid) {
    }
}
