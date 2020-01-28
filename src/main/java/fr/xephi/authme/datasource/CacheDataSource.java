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
import fr.xephi.authme.ThreadSafety;
import fr.xephi.authme.annotation.ShouldBeAsync;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.output.ConsoleLoggerFactory;
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

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(CacheDataSource.class);

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
    public boolean isCached() {
        return true;
    }

    @Override
    @ShouldBeAsync
    public boolean isAuthAvailable(String user) {
        ThreadSafety.shouldBeAsync();
        return getAuth(user) != null;
    }

    @Override
    @ShouldBeAsync
    public HashedPassword getPassword(String user) {
        ThreadSafety.shouldBeAsync();
        user = user.toLowerCase();
        Optional<PlayerAuth> pAuthOpt = cachedAuths.getIfPresent(user);
        if (pAuthOpt != null && pAuthOpt.isPresent()) {
            return pAuthOpt.get().getPassword();
        }
        return source.getPassword(user);
    }

    @Override
    @ShouldBeAsync
    public PlayerAuth getAuth(String user) {
        ThreadSafety.shouldBeAsync();
        user = user.toLowerCase();
        return cachedAuths.getUnchecked(user).orElse(null);
    }

    @Override
    @ShouldBeAsync
    public boolean saveAuth(PlayerAuth auth) {
        ThreadSafety.shouldBeAsync();
        boolean result = source.saveAuth(auth);
        if (result) {
            cachedAuths.refresh(auth.getNickname());
        }
        return result;
    }

    @Override
    @ShouldBeAsync
    public boolean updatePassword(PlayerAuth auth) {
        ThreadSafety.shouldBeAsync();
        boolean result = source.updatePassword(auth);
        if (result) {
            cachedAuths.refresh(auth.getNickname());
        }
        return result;
    }

    @Override
    @ShouldBeAsync
    public boolean updatePassword(String user, HashedPassword password) {
        ThreadSafety.shouldBeAsync();
        user = user.toLowerCase();
        boolean result = source.updatePassword(user, password);
        if (result) {
            cachedAuths.refresh(user);
        }
        return result;
    }

    @Override
    @ShouldBeAsync
    public boolean updateSession(PlayerAuth auth) {
        ThreadSafety.shouldBeAsync();
        boolean result = source.updateSession(auth);
        if (result) {
            cachedAuths.refresh(auth.getNickname());
        }
        return result;
    }

    @Override
    @ShouldBeAsync
    public boolean updateQuitLoc(final PlayerAuth auth) {
        ThreadSafety.shouldBeAsync();
        boolean result = source.updateQuitLoc(auth);
        if (result) {
            cachedAuths.refresh(auth.getNickname());
        }
        return result;
    }

    @Override
    @ShouldBeAsync
    public Set<String> getRecordsToPurge(long until) {
        ThreadSafety.shouldBeAsync();
        return source.getRecordsToPurge(until);
    }

    @Override
    @ShouldBeAsync
    public boolean removeAuth(String name) {
        ThreadSafety.shouldBeAsync();
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
            logger.logException("Could not close executor service:", e);
        }
        cachedAuths.invalidateAll();
        source.closeConnection();
    }

    @Override
    @ShouldBeAsync
    public boolean updateEmail(final PlayerAuth auth) {
        ThreadSafety.shouldBeAsync();
        boolean result = source.updateEmail(auth);
        if (result) {
            cachedAuths.refresh(auth.getNickname());
        }
        return result;
    }

    @Override
    @ShouldBeAsync
    public List<String> getAllAuthsByIp(String ip) {
        ThreadSafety.shouldBeAsync();
        return source.getAllAuthsByIp(ip);
    }

    @Override
    @ShouldBeAsync
    public int countAuthsByEmail(String email) {
        ThreadSafety.shouldBeAsync();
        return source.countAuthsByEmail(email);
    }

    @Override
    @ShouldBeAsync
    public void purgeRecords(Collection<String> banned) {
        ThreadSafety.shouldBeAsync();
        source.purgeRecords(banned);
        cachedAuths.invalidateAll(banned);
    }

    @Override
    public DataSourceType getType() {
        ThreadSafety.shouldBeAsync();
        return source.getType();
    }

    @Override
    @ShouldBeAsync
    public boolean isLogged(String user) {
        ThreadSafety.shouldBeAsync();
        return source.isLogged(user);
    }

    @Override
    @ShouldBeAsync
    public void setLogged(final String user) {
        ThreadSafety.shouldBeAsync();
        source.setLogged(user.toLowerCase());
    }

    @Override
    @ShouldBeAsync
    public void setUnlogged(final String user) {
        ThreadSafety.shouldBeAsync();
        source.setUnlogged(user.toLowerCase());
    }

    @Override
    @ShouldBeAsync
    public boolean hasSession(final String user) {
        ThreadSafety.shouldBeAsync();
        return source.hasSession(user);
    }

    @Override
    @ShouldBeAsync
    public void grantSession(final String user) {
        ThreadSafety.shouldBeAsync();
        source.grantSession(user);
    }

    @Override
    @ShouldBeAsync
    public void revokeSession(final String user) {
        ThreadSafety.shouldBeAsync();
        source.revokeSession(user);
    }

    @Override
    @ShouldBeAsync
    public void purgeLogged() {
        ThreadSafety.shouldBeAsync();
        source.purgeLogged();
        cachedAuths.invalidateAll();
    }

    @Override
    @ShouldBeAsync
    public int getAccountsRegistered() {
        ThreadSafety.shouldBeAsync();
        return source.getAccountsRegistered();
    }

    @Override
    @ShouldBeAsync
    public boolean updateRealName(String user, String realName) {
        ThreadSafety.shouldBeAsync();
        boolean result = source.updateRealName(user, realName);
        if (result) {
            cachedAuths.refresh(user);
        }
        return result;
    }

    @Override
    @ShouldBeAsync
    public DataSourceValue<String> getEmail(String user) {
        ThreadSafety.shouldBeAsync();
        return cachedAuths.getUnchecked(user)
            .map(auth -> DataSourceValueImpl.of(auth.getEmail()))
            .orElse(DataSourceValueImpl.unknownRow());
    }

    @Override
    @ShouldBeAsync
    public List<PlayerAuth> getAllAuths() {
        ThreadSafety.shouldBeAsync();
        return source.getAllAuths();
    }

    @Override
    @ShouldBeAsync
    public List<String> getLoggedPlayersWithEmptyMail() {
        ThreadSafety.shouldBeAsync();
        return playerCache.getCache().values().stream()
            .filter(auth -> Utils.isEmailEmpty(auth.getEmail()))
            .map(PlayerAuth::getRealName)
            .collect(Collectors.toList());
    }

    @Override
    @ShouldBeAsync
    public List<PlayerAuth> getRecentlyLoggedInPlayers() {
        ThreadSafety.shouldBeAsync();
        return source.getRecentlyLoggedInPlayers();
    }

    @Override
    @ShouldBeAsync
    public boolean setTotpKey(String user, String totpKey) {
        ThreadSafety.shouldBeAsync();
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
    @ShouldBeAsync
    public void refreshCache(String playerName) {
        ThreadSafety.shouldBeAsync();
        if (cachedAuths.getIfPresent(playerName) != null) {
            cachedAuths.refresh(playerName);
        }
    }

}
