package fr.xephi.authme.datasource;

import ch.jalu.datasourcecolumns.data.DataSourceValue;
import ch.jalu.datasourcecolumns.data.DataSourceValueImpl;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.util.Utils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CacheDataSource implements DataSource {

    private final DataSource source;
    private final PlayerCache playerCache;
    private final LoadingCache<String, Optional<PlayerAuth>> cachedAuths;
    private final ListeningExecutorService executorService;

    /**
     * Constructor for CacheDataSource.
     *
     * @param source the source
     * @param playerCache the player cache
     */
    public CacheDataSource(DataSource source, PlayerCache playerCache) {
        this.source = source;
        this.playerCache = playerCache;

        executorService = MoreExecutors.listeningDecorator(
            Executors.newCachedThreadPool(new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("AuthMe-CacheLoader")
                .build())
        );
        cachedAuths = CacheBuilder.newBuilder()
            .refreshAfterWrite(5, TimeUnit.MINUTES)
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Optional<PlayerAuth>>() {
                @Override
                public Optional<PlayerAuth> load(String key) {
                    return Optional.ofNullable(source.getAuth(key));
                }

                @Override
                public ListenableFuture<Optional<PlayerAuth>> reload(final String key, Optional<PlayerAuth> oldValue) {
                    return executorService.submit(() -> load(key));
                }
            });
    }

    public LoadingCache<String, Optional<PlayerAuth>> getCachedAuths() {
        return cachedAuths;
    }

    @Override
    public void reload() {
        source.reload();
    }

    @Override
    public boolean isAuthAvailable(String user) {
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
    public PlayerAuth getAuth(String user) {
        user = user.toLowerCase();
        return cachedAuths.getUnchecked(user).orElse(null);
    }

    @Override
    public boolean saveAuth(PlayerAuth auth) {
        boolean result = source.saveAuth(auth);
        if (result) {
            cachedAuths.refresh(auth.getNickname());
        }
        return result;
    }

    @Override
    public boolean updatePassword(PlayerAuth auth) {
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
    public Set<String> getRecordsToPurge(long until) {
        return source.getRecordsToPurge(until);
    }

    @Override
    public boolean removeAuth(String name) {
        name = name.toLowerCase();
        boolean result = source.removeAuth(name);
        if (result) {
            cachedAuths.invalidate(name);
        }
        return result;
    }

    @Override
    public void closeConnection() {
        executorService.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            ConsoleLogger.logException("Could not close executor service:", e);
        }
        cachedAuths.invalidateAll();
        source.closeConnection();
    }

    @Override
    public boolean updateEmail(final PlayerAuth auth) {
        boolean result = source.updateEmail(auth);
        if (result) {
            cachedAuths.refresh(auth.getNickname());
        }
        return result;
    }

    @Override
    public List<String> getAllAuthsByIp(String ip) {
        return source.getAllAuthsByIp(ip);
    }

    @Override
    public int countAuthsByEmail(String email) {
        return source.countAuthsByEmail(email);
    }

    @Override
    public void purgeRecords(Collection<String> banned) {
        source.purgeRecords(banned);
        cachedAuths.invalidateAll(banned);
    }

    @Override
    public DataSourceType getType() {
        return source.getType();
    }

    @Override
    public boolean isLogged(String user) {
        return source.isLogged(user);
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
    public boolean hasSession(final String user) {
        return source.hasSession(user);
    }

    @Override
    public void grantSession(final String user) {
        source.grantSession(user);
    }

    @Override
    public void revokeSession(final String user) {
        source.revokeSession(user);
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
    public boolean updateRealName(String user, String realName) {
        boolean result = source.updateRealName(user, realName);
        if (result) {
            cachedAuths.refresh(user);
        }
        return result;
    }

    @Override
    public DataSourceValue<String> getEmail(String user) {
        return cachedAuths.getUnchecked(user)
            .map(auth -> DataSourceValueImpl.of(auth.getEmail()))
            .orElse(DataSourceValueImpl.unknownRow());
    }

    @Override
    public List<PlayerAuth> getAllAuths() {
        return source.getAllAuths();
    }

    @Override
    public List<String> getLoggedPlayersWithEmptyMail() {
        return playerCache.getCache().values().stream()
            .filter(auth -> Utils.isEmailEmpty(auth.getEmail()))
            .map(PlayerAuth::getRealName)
            .collect(Collectors.toList());
    }

    @Override
    public List<PlayerAuth> getRecentlyLoggedInPlayers() {
        return source.getRecentlyLoggedInPlayers();
    }

    @Override
    public boolean setTotpKey(String user, String totpKey) {
        boolean result = source.setTotpKey(user, totpKey);
        if (result) {
            cachedAuths.refresh(user);
        }
        return result;
    }

    @Override
    public void invalidateCache(String playerName) {
        cachedAuths.invalidate(playerName);
    }

    @Override
    public void refreshCache(String playerName) {
        if (cachedAuths.getIfPresent(playerName) != null) {
            cachedAuths.refresh(playerName);
        }
    }

}
