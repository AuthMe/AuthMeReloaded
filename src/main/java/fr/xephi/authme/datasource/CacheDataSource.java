package fr.xephi.authme.datasource;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalListeners;
import com.google.common.cache.RemovalNotification;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 */
public class CacheDataSource implements DataSource {

    private final DataSource source;
    private final ExecutorService exec;
    private final LoadingCache<String, Optional<PlayerAuth>> cachedAuths;

    /**
     * Constructor for CacheDataSource.
     *
     * @param src DataSource
     */
    public CacheDataSource(DataSource src) {
        this.source = src;
        this.exec = Executors.newCachedThreadPool();
        cachedAuths = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .removalListener(RemovalListeners.asynchronous(new RemovalListener<String, Optional<PlayerAuth>>() {
                @Override
                public void onRemoval(RemovalNotification<String, Optional<PlayerAuth>> removalNotification) {
                    String name = removalNotification.getKey();
                    if (PlayerCache.getInstance().isAuthenticated(name)) {
                        cachedAuths.getUnchecked(name);
                    }
                }
            }, exec))
            .build(
                new CacheLoader<String, Optional<PlayerAuth>>() {
                    public Optional<PlayerAuth> load(String key) {
                        return Optional.fromNullable(source.getAuth(key));
                    }
                });
    }

    /**
     * Method isAuthAvailable.
     *
     * @param user String
     *
     * @return boolean
     *
     * @see fr.xephi.authme.datasource.DataSource#isAuthAvailable(String)
     */
    @Override
    public synchronized boolean isAuthAvailable(String user) {
        return getAuth(user) != null;
    }

    /**
     * Method getAuth.
     *
     * @param user String
     *
     * @return PlayerAuth
     *
     * @see fr.xephi.authme.datasource.DataSource#getAuth(String)
     */
    @Override
    public synchronized PlayerAuth getAuth(String user) {
        user = user.toLowerCase();
        return cachedAuths.getUnchecked(user).orNull();
    }

    /**
     * Method saveAuth.
     *
     * @param auth PlayerAuth
     *
     * @return boolean
     *
     * @see fr.xephi.authme.datasource.DataSource#saveAuth(PlayerAuth)
     */
    @Override
    public synchronized boolean saveAuth(PlayerAuth auth) {
        boolean result = source.saveAuth(auth);
        if (result) {
            cachedAuths.refresh(auth.getNickname());
        }
        return result;
    }

    /**
     * Method updatePassword.
     *
     * @param auth PlayerAuth
     *
     * @return boolean
     *
     * @see fr.xephi.authme.datasource.DataSource#updatePassword(PlayerAuth)
     */
    @Override
    public synchronized boolean updatePassword(PlayerAuth auth) {
        boolean result = source.updatePassword(auth);
        if (result) {
            cachedAuths.refresh(auth.getNickname());
        }
        return result;
    }

    /**
     * Method updateSession.
     *
     * @param auth PlayerAuth
     *
     * @return boolean
     *
     * @see fr.xephi.authme.datasource.DataSource#updateSession(PlayerAuth)
     */
    @Override
    public boolean updateSession(PlayerAuth auth) {
        boolean result = source.updateSession(auth);
        if (result) {
            cachedAuths.refresh(auth.getNickname());
        }
        return result;
    }

    /**
     * Method updateQuitLoc.
     *
     * @param auth PlayerAuth
     *
     * @return boolean
     *
     * @see fr.xephi.authme.datasource.DataSource#updateQuitLoc(PlayerAuth)
     */
    @Override
    public boolean updateQuitLoc(final PlayerAuth auth) {
        boolean result = source.updateSession(auth);
        if (result) {
            cachedAuths.refresh(auth.getNickname());
        }
        return result;
    }

    /**
     * Method getIps.
     *
     * @param ip String
     *
     * @return int
     *
     * @see fr.xephi.authme.datasource.DataSource#getIps(String)
     */
    @Override
    public int getIps(String ip) {
        return source.getIps(ip);
    }

    /**
     * Method purgeDatabase.
     *
     * @param until long
     *
     * @return int
     *
     * @see fr.xephi.authme.datasource.DataSource#purgeDatabase(long)
     */
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

    /**
     * Method autoPurgeDatabase.
     *
     * @param until long
     *
     * @return List
     *
     * @see fr.xephi.authme.datasource.DataSource#autoPurgeDatabase(long)
     */
    @Override
    public List<String> autoPurgeDatabase(long until) {
        List<String> cleared = source.autoPurgeDatabase(until);
        for (String name : cleared) {
            cachedAuths.invalidate(name);
        }
        return cleared;
    }

    /**
     * Method removeAuth.
     *
     * @param name String
     *
     * @return boolean
     *
     * @see fr.xephi.authme.datasource.DataSource#removeAuth(String)
     */
    @Override
    public synchronized boolean removeAuth(String name) {
        name = name.toLowerCase();
        boolean result = source.removeAuth(name);
        if (result) {
            cachedAuths.invalidate(name);
        }
        return result;
    }

    /**
     * Method close.
     *
     * @see fr.xephi.authme.datasource.DataSource#close()
     */
    @Override
    public synchronized void close() {
        exec.shutdown();
        source.close();
    }

    /**
     * Method reload.
     *
     * @see fr.xephi.authme.datasource.DataSource#reload()
     */
    @Override
    public void reload() { // unused method
        exec.execute(new Runnable() {
            @Override
            public void run() {
                source.reload();
                cachedAuths.invalidateAll();
            }
        });
    }

    /**
     * Method updateEmail.
     *
     * @param auth PlayerAuth
     *
     * @return boolean
     *
     * @see fr.xephi.authme.datasource.DataSource#updateEmail(PlayerAuth)
     */
    @Override
    public synchronized boolean updateEmail(final PlayerAuth auth) {
        boolean result = source.updateEmail(auth);
        if (result) {
            cachedAuths.refresh(auth.getNickname());
        }
        return result;
    }

    /**
     * Method updateSalt.
     *
     * @param auth PlayerAuth
     *
     * @return boolean
     *
     * @see fr.xephi.authme.datasource.DataSource#updateSalt(PlayerAuth)
     */
    @Override
    public synchronized boolean updateSalt(final PlayerAuth auth) {
        boolean result = source.updateSalt(auth);
        if (result) {
            cachedAuths.refresh(auth.getNickname());
        }
        return result;
    }

    /**
     * Method getAllAuthsByName.
     *
     * @param auth PlayerAuth
     *
     * @return List
     *
     * @see fr.xephi.authme.datasource.DataSource#getAllAuthsByName(PlayerAuth)
     */
    @Override
    public synchronized List<String> getAllAuthsByName(PlayerAuth auth) {
        return source.getAllAuthsByName(auth);
    }

    /**
     * Method getAllAuthsByIp.
     *
     * @param ip String
     *
     * @return List
     *
     * @see fr.xephi.authme.datasource.DataSource#getAllAuthsByIp(String)
     */
    @Override
    public synchronized List<String> getAllAuthsByIp(final String ip) {
        return source.getAllAuthsByIp(ip);
    }

    /**
     * Method getAllAuthsByEmail.
     *
     * @param email String
     *
     * @return List
     *
     * @see fr.xephi.authme.datasource.DataSource#getAllAuthsByEmail(String)
     */
    @Override
    public synchronized List<String> getAllAuthsByEmail(final String email) {
        return source.getAllAuthsByEmail(email);
    }

    /**
     * Method purgeBanned.
     *
     * @param banned List<String>
     *
     * @see fr.xephi.authme.datasource.DataSource#purgeBanned(List)
     */
    @Override
    public synchronized void purgeBanned(final List<String> banned) {
        exec.execute(new Runnable() {
            @Override
            public void run() {
                source.purgeBanned(banned);
                cachedAuths.invalidateAll(banned);
            }
        });
    }

    /**
     * Method getType.
     *
     * @return DataSourceType
     *
     * @see fr.xephi.authme.datasource.DataSource#getType()
     */
    @Override
    public DataSourceType getType() {
        return source.getType();
    }

    /**
     * Method isLogged.
     *
     * @param user String
     *
     * @return boolean
     *
     * @see fr.xephi.authme.datasource.DataSource#isLogged(String)
     */
    @Override
    public boolean isLogged(String user) {
        return PlayerCache.getInstance().isAuthenticated(user);
    }

    /**
     * Method setLogged.
     *
     * @param user String
     *
     * @see fr.xephi.authme.datasource.DataSource#setLogged(String)
     */
    @Override
    public void setLogged(final String user) {
        exec.execute(new Runnable() {
            @Override
            public void run() {
                source.setLogged(user.toLowerCase());
            }
        });
    }

    /**
     * Method setUnlogged.
     *
     * @param user String
     *
     * @see fr.xephi.authme.datasource.DataSource#setUnlogged(String)
     */
    @Override
    public void setUnlogged(final String user) {
        exec.execute(new Runnable() {
            @Override
            public void run() {
                source.setUnlogged(user.toLowerCase());
            }
        });
    }

    /**
     * Method purgeLogged.
     *
     * @see fr.xephi.authme.datasource.DataSource#purgeLogged()
     */
    @Override
    public void purgeLogged() {
        exec.execute(new Runnable() {
            @Override
            public void run() {
                source.purgeLogged();
                cachedAuths.invalidateAll();
            }
        });
    }

    /**
     * Method getAccountsRegistered.
     *
     * @return int
     *
     * @see fr.xephi.authme.datasource.DataSource#getAccountsRegistered()
     */
    @Override
    public int getAccountsRegistered() {
        return source.getAccountsRegistered();
    }

    /**
     * Method updateName.
     *
     * @param oldOne String
     * @param newOne String
     *
     * @see fr.xephi.authme.datasource.DataSource#updateName(String, String)
     */
    @Override
    public void updateName(final String oldOne, final String newOne) {
        exec.execute(new Runnable() {
            @Override
            public void run() {
                source.updateName(oldOne, newOne);
                cachedAuths.invalidate(oldOne);
            }
        });
    }

    /**
     * Method getAllAuths.
     *
     * @return List
     *
     * @see fr.xephi.authme.datasource.DataSource#getAllAuths()
     */
    @Override
    public List<PlayerAuth> getAllAuths() {
        return source.getAllAuths();
    }

    /**
     * Method getLoggedPlayers.
     *
     * @return List
     *
     * @see fr.xephi.authme.datasource.DataSource#getLoggedPlayers()
     */
    @Override
    public List<PlayerAuth> getLoggedPlayers() {
        return new ArrayList<>(PlayerCache.getInstance().getCache().values());
    }

	@Override
	public Connection getConnection() throws SQLException {
		return source.getConnection();
	}
}
