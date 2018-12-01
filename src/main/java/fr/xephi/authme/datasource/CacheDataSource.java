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
import fr.xephi.authme.data.player.NamedIdentifier;
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
    private final LoadingCache<NamedIdentifier, Optional<PlayerAuth>> cachedAuths;
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
            .build(new CacheLoader<NamedIdentifier, Optional<PlayerAuth>>() {
                @Override
                public Optional<PlayerAuth> load(NamedIdentifier key) {
                    return Optional.ofNullable(source.getAuth(key));
                }

                @Override
                public ListenableFuture<Optional<PlayerAuth>> reload(final NamedIdentifier key, Optional<PlayerAuth> oldValue) {
                    return executorService.submit(() -> load(key));
                }
            });
    }

    public LoadingCache<NamedIdentifier, Optional<PlayerAuth>> getCachedAuths() {
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
    public boolean isAuthAvailable(NamedIdentifier identifier) {
        return getAuth(identifier) != null;
    }

    @Override
    public HashedPassword getPassword(NamedIdentifier identifier) {
        Optional<PlayerAuth> pAuthOpt = cachedAuths.getIfPresent(identifier);
        if (pAuthOpt != null && pAuthOpt.isPresent()) {
            return pAuthOpt.get().getPassword();
        }
        return source.getPassword(identifier);
    }

    @Override
    public PlayerAuth getAuth(NamedIdentifier identifier) {
        return cachedAuths.getUnchecked(identifier).orElse(null);
    }

    @Override
    public boolean saveAuth(PlayerAuth auth) {
        boolean result = source.saveAuth(auth);
        if (result) {
            cachedAuths.refresh(auth.toIdentifier());
        }
        return result;
    }

    @Override
    public boolean updatePassword(PlayerAuth auth) {
        boolean result = source.updatePassword(auth);
        if (result) {
            cachedAuths.refresh(auth.toIdentifier());
        }
        return result;
    }

    @Override
    public boolean updatePassword(NamedIdentifier identifier, HashedPassword password) {
        boolean result = source.updatePassword(identifier, password);
        if (result) {
            cachedAuths.refresh(identifier);
        }
        return result;
    }

    @Override
    public boolean updateSession(PlayerAuth auth) {
        boolean result = source.updateSession(auth);
        if (result) {
            cachedAuths.refresh(auth.toIdentifier());
        }
        return result;
    }

    @Override
    public boolean updateQuitLoc(final PlayerAuth auth) {
        boolean result = source.updateQuitLoc(auth);
        if (result) {
            cachedAuths.refresh(auth.toIdentifier());
        }
        return result;
    }

    @Override
    public Set<String> getRecordsToPurge(long until) {
        return source.getRecordsToPurge(until);
    }

    @Override
    public boolean removeAuth(NamedIdentifier identifier) {
        boolean result = source.removeAuth(identifier);
        if (result) {
            cachedAuths.invalidate(identifier);
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
            cachedAuths.refresh(auth.toIdentifier());
        }
        return result;
    }

    @Override
    public List<NamedIdentifier> getAllAuthsByIp(String ip) {
        return source.getAllAuthsByIp(ip);
    }

    @Override
    public int countAuthsByEmail(String email) {
        return source.countAuthsByEmail(email);
    }

    @Override
    public void purgeRecords(Collection<NamedIdentifier> toPurge) {
        source.purgeRecords(toPurge);
        cachedAuths.invalidateAll(toPurge);
    }

    @Override
    public DataSourceType getType() {
        return source.getType();
    }

    @Override
    public boolean isLogged(NamedIdentifier identifier) {
        return source.isLogged(identifier);
    }

    @Override
    public void setLogged(final NamedIdentifier identifier) {
        source.setLogged(identifier);
    }

    @Override
    public void setUnlogged(final NamedIdentifier identifier) {
        source.setUnlogged(identifier);
    }

    @Override
    public boolean hasSession(final NamedIdentifier identifier) {
        return source.hasSession(identifier);
    }

    @Override
    public void grantSession(final NamedIdentifier identifier) {
        source.grantSession(identifier);
    }

    @Override
    public void revokeSession(final NamedIdentifier identifier) {
        source.revokeSession(identifier);
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
    public boolean updateRealName(NamedIdentifier identifier) {
        boolean result = source.updateRealName(identifier);
        if (result) {
            cachedAuths.refresh(identifier);
        }
        return result;
    }

    @Override
    public DataSourceValue<String> getEmail(NamedIdentifier identifier) {
        return cachedAuths.getUnchecked(identifier)
            .map(auth -> DataSourceValueImpl.of(auth.getEmail()))
            .orElse(DataSourceValueImpl.unknownRow());
    }

    @Override
    public List<PlayerAuth> getAllAuths() {
        return source.getAllAuths();
    }

    @Override
    public List<NamedIdentifier> getLoggedPlayersWithEmptyMail() {
        return playerCache.getCache().values().stream()
            .filter(auth -> Utils.isEmailEmpty(auth.getEmail()))
            .map(PlayerAuth::toIdentifier)
            .collect(Collectors.toList());
    }

    @Override
    public List<PlayerAuth> getRecentlyLoggedInPlayers() {
        return source.getRecentlyLoggedInPlayers();
    }

    @Override
    public boolean setTotpKey(NamedIdentifier identifier, String totpKey) {
        boolean result = source.setTotpKey(identifier, totpKey);
        if (result) {
            cachedAuths.refresh(identifier);
        }
        return result;
    }

    @Override
    public void invalidateCache(NamedIdentifier identifier) {
        cachedAuths.invalidate(identifier);
    }

    @Override
    public void refreshCache(NamedIdentifier identifier) {
        if (cachedAuths.getIfPresent(identifier) != null) {
            cachedAuths.refresh(identifier);
        }
    }

}
