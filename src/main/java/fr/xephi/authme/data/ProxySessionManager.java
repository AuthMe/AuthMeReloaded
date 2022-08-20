package fr.xephi.authme.data;

import fr.xephi.authme.initialization.HasCleanup;
import fr.xephi.authme.util.expiring.ExpiringSet;

import javax.inject.Inject;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ProxySessionManager implements HasCleanup {

    private final ExpiringSet<String> activeProxySessions;

    @Inject
    public ProxySessionManager() {
        long countTimeout = 5;
        activeProxySessions = new ExpiringSet<>(countTimeout, TimeUnit.SECONDS);
    }

    /**
     * Saves the player in the set
     * @param name the player's name
     */
    private void setActiveSession(String name) {
        activeProxySessions.add(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Process a proxy session message from AuthMeBungee
     * @param name the player to process
     */
    public void processProxySessionMessage(String name) {
        setActiveSession(name);
    }

    /**
     * Returns if the player should be logged in or not
     * @param name the name of the player to check
     * @return true if player has to be logged in, false otherwise
     */
    public boolean shouldResumeSession(String name) {
        return activeProxySessions.contains(name);
    }

    @Override
    public void performCleanup() {
        activeProxySessions.removeExpiredEntries();
    }
}
