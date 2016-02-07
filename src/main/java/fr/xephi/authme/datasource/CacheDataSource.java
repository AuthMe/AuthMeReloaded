package fr.xephi.authme.datasource;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.security.crypts.HashedPassword;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 */
public class CacheDataSource implements DataSource {

    private final DataSource source;
    private final LoadingCache<String, Optional<PlayerAuth>> cachedAuths;

    /**
     * Constructor for CacheDataSource.
     *
     * @param src DataSource
     */
    public CacheDataSource(DataSource src) {
        this.source = src;
        this.cachedAuths = CacheBuilder.newBuilder()
            .expireAfterWrite(8, TimeUnit.MINUTES)
            .removalListener(new RemovalListener<String, Optional<PlayerAuth>>() {
                @Override
                public void onRemoval(RemovalNotification<String, Optional<PlayerAuth>> removalNotification) {
                    String name = removalNotification.getKey();
                    if (PlayerCache.getInstance().isAuthenticated(name)) {
                        cachedAuths.getUnchecked(name);
                    }
                }
            })
            .build(
                new CacheLoader<String, Optional<PlayerAuth>>() {
                    @Override
                    public Optional<PlayerAuth> load(String key) {
                        return Optional.fromNullable(source.getAuth(key));
                    }
                });
    }

    @Override
    public synchronized boolean isAuthAvailable(String user) {
        return getAuth(user) != null;
    }

    @Override
    public HashedPassword getPassword(String user) {
        user = user.toLowerCase();
        Optional<PlayerAuth> pAuthOpt = cachedAuths.getIfPresent(user);
        if (pAuthOpt != null && pAuthOpt.isPresent()) {
            return pAuthOpt.get().getPassword();
        }
        return source.getPassword(user);
    }

    @Override
    public synchronized PlayerAuth getAuth(String user) {
        user = user.toLowerCase();
        return cachedAuths.getUnchecked(user).orNull();
    }

    @Override
    public synchronized boolean saveAuth(PlayerAuth auth) {
        boolean result = source.saveAuth(auth);
        if (result) {
            cachedAuths.refresh(auth.getNickname());
        }
        return result;
    }

    @Override
    public synchronized boolean updatePassword(PlayerAuth auth) {
        boolean result = source.updatePassword(auth);
        if (result) {
            cachedAuths.refresh(auth.getNickname());
        }
        return result;
    }

    @Override
    public boolean updatePassword(String user, HashedPassword password) {
        user = user.toLowerCase();
        boolean result = source.updatePassword(user, password);
        if (result) {
            cachedAuths.refresh(user);
        }
        return result;
    }

    @Override
    public boolean updateSession(PlayerAuth auth) {
        boolean result = source.updateSession(auth);
        if (result) {
            cachedAuths.refresh(auth.getNickname());
        }
        return result;
    }

    @Override
    public boolean updateQuitLoc(final PlayerAuth auth) {
        boolean result = source.updateQuitLoc(auth);
        if (result) {
            cachedAuths.refresh(auth.getNickname());
        }
        return result;
    }

    @Override
    public int getIps(String ip) {
        return source.getIps(ip);
    }

    @Override
    public int purgeDatabase(long until) {
        int cleared = source.purgeDatabase(until);
        if (cleared > 0) {
            for (Optional<PlayerAuth> auth : cachedAuths.asMap().values()) {
                if (auth.isPresent() && auth.get().getLastLogin() < until) {
                    cachedAuths.invalidate(auth.get().getNickname());
                }
            }
        }
        return cleared;
    }

    @Override
    public List<String> autoPurgeDatabase(long until) {
        List<String> cleared = source.autoPurgeDatabase(until);
        for (String name : cleared) {
            cachedAuths.invalidate(name);
        }
        return cleared;
    }

    @Override
    public synchronized boolean removeAuth(String name) {
        name = name.toLowerCase();
        boolean result = source.removeAuth(name);
        if (result) {
            cachedAuths.invalidate(name);
        }
        return result;
    }

    @Override
    public synchronized void close() {
        source.close();
    }

    @Override
    public void reload() { // unused method
        source.reload();
        cachedAuths.invalidateAll();
    }

    @Override
    public synchronized boolean updateEmail(final PlayerAuth auth) {
        boolean result = source.updateEmail(auth);
        if (result) {
            cachedAuths.refresh(auth.getNickname());
        }
        return result;
    }

    @Override
    public synchronized List<String> getAllAuthsByName(PlayerAuth auth) {
        return source.getAllAuthsByName(auth);
    }

    @Override
    public synchronized List<String> getAllAuthsByIp(final String ip) {
        return source.getAllAuthsByIp(ip);
    }

    @Override
    public synchronized List<String> getAllAuthsByEmail(final String email) {
        return source.getAllAuthsByEmail(email);
    }

    @Override
    public synchronized void purgeBanned(final List<String> banned) {
        source.purgeBanned(banned);
        cachedAuths.invalidateAll(banned);
    }

    @Override
    public DataSourceType getType() {
        return source.getType();
    }

    @Override
    public boolean isLogged(String user) {
        return PlayerCache.getInstance().isAuthenticated(user);
    }

    @Override
    public void setLogged(final String user) {
        source.setLogged(user.toLowerCase());
    }

    @Override
    public void setUnlogged(final String user) {
        source.setUnlogged(user.toLowerCase());
    }

    @Override
    public void purgeLogged() {
        source.purgeLogged();
        cachedAuths.invalidateAll();
    }

    @Override
    public int getAccountsRegistered() {
        return source.getAccountsRegistered();
    }

    @Override
    public void updateName(final String oldOne, final String newOne) { // unused method
        source.updateName(oldOne, newOne);
        cachedAuths.invalidate(oldOne);
    }

    @Override
    public List<PlayerAuth> getAllAuths() {
        return source.getAllAuths();
    }

    @Override
    public List<PlayerAuth> getLoggedPlayers() {
        return new ArrayList<>(PlayerCache.getInstance().getCache().values());
    }

    @Override
    public boolean isEmailStored(String email) {
        return source.isEmailStored(email);
    }
}
