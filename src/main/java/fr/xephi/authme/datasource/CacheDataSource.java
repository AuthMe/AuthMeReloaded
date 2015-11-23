package fr.xephi.authme.datasource;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.util.Utils;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 */
public class CacheDataSource implements DataSource {

    private final DataSource source;
    private final ExecutorService exec;
    private final ConcurrentHashMap<String, PlayerAuth> cache = new ConcurrentHashMap<>();

    /**
     * Constructor for CacheDataSource.
     *
     * @param pl  AuthMe
     * @param src DataSource
     */
    public CacheDataSource(final AuthMe pl, DataSource src) {
        this.source = src;
        this.exec = Executors.newCachedThreadPool();
        pl.setCanConnect(false);

        /*
         * We need to load all players in cache ... It will took more time to
         * load the server, but it will be much easier to check for an
         * isAuthAvailable !
         */
        exec.execute(new Runnable() {
            @Override
            public void run() {
                for (PlayerAuth auth : source.getAllAuths()) {
                    cache.put(auth.getNickname().toLowerCase(), auth);
                }
                pl.setCanConnect(true);
            }
        });
    }

    /**
     * Method isAuthAvailable.
     *
     * @param user String
     * @return boolean * @see fr.xephi.authme.datasource.DataSource#isAuthAvailable(String)
     */
    @Override
    public synchronized boolean isAuthAvailable(String user) {
        return cache.containsKey(user.toLowerCase());
    }

    /**
     * Method getAuth.
     *
     * @param user String
     * @return PlayerAuth * @see fr.xephi.authme.datasource.DataSource#getAuth(String)
     */
    @Override
    public synchronized PlayerAuth getAuth(String user) {
        user = user.toLowerCase();
        if (cache.containsKey(user)) {
            return cache.get(user);
        }
        return null;
    }

    /**
     * Method saveAuth.
     *
     * @param auth PlayerAuth
     * @return boolean * @see fr.xephi.authme.datasource.DataSource#saveAuth(PlayerAuth)
     */
    @Override
    public synchronized boolean saveAuth(final PlayerAuth auth) {
        cache.put(auth.getNickname(), auth);
        exec.execute(new Runnable() {
            @Override
            public void run() {
                if (!source.saveAuth(auth)) {
                    cache.remove(auth.getNickname());
                }
            }
        });
        return true;
    }

    /**
     * Method updatePassword.
     *
     * @param auth PlayerAuth
     * @return boolean * @see fr.xephi.authme.datasource.DataSource#updatePassword(PlayerAuth)
     */
    @Override
    public synchronized boolean updatePassword(final PlayerAuth auth) {
        if (!cache.containsKey(auth.getNickname())) {
            return false;
        }
        final String oldHash = cache.get(auth.getNickname()).getHash();
        cache.get(auth.getNickname()).setHash(auth.getHash());
        exec.execute(new Runnable() {
            @Override
            public void run() {
                if (!source.updatePassword(auth)) {
                    if (cache.containsKey(auth.getNickname())) {
                        cache.get(auth.getNickname()).setHash(oldHash);
                    }
                }
            }
        });
        return true;
    }

    /**
     * Method updateSession.
     *
     * @param auth PlayerAuth
     * @return boolean * @see fr.xephi.authme.datasource.DataSource#updateSession(PlayerAuth)
     */
    @Override
    public boolean updateSession(final PlayerAuth auth) {
        if (!cache.containsKey(auth.getNickname())) {
            return false;
        }
        PlayerAuth cachedAuth = cache.get(auth.getNickname());
        final String oldIp = cachedAuth.getIp();
        final long oldLastLogin = cachedAuth.getLastLogin();
        final String oldRealName = cachedAuth.getRealName();

        cachedAuth.setIp(auth.getIp());
        cachedAuth.setLastLogin(auth.getLastLogin());
        cachedAuth.setRealName(auth.getRealName());
        exec.execute(new Runnable() {
            @Override
            public void run() {
                if (!source.updateSession(auth)) {
                    if (cache.containsKey(auth.getNickname())) {
                        PlayerAuth cachedAuth = cache.get(auth.getNickname());
                        cachedAuth.setIp(oldIp);
                        cachedAuth.setLastLogin(oldLastLogin);
                        cachedAuth.setRealName(oldRealName);
                    }
                }
            }
        });
        return true;
    }

    /**
     * Method updateQuitLoc.
     *
     * @param auth PlayerAuth
     * @return boolean * @see fr.xephi.authme.datasource.DataSource#updateQuitLoc(PlayerAuth)
     */
    @Override
    public boolean updateQuitLoc(final PlayerAuth auth) {
        if (!cache.containsKey(auth.getNickname())) {
            return false;
        }
        final PlayerAuth cachedAuth = cache.get(auth.getNickname());
        final double oldX = cachedAuth.getQuitLocX();
        final double oldY = cachedAuth.getQuitLocY();
        final double oldZ = cachedAuth.getQuitLocZ();
        final String oldWorld = cachedAuth.getWorld();

        cachedAuth.setQuitLocX(auth.getQuitLocX());
        cachedAuth.setQuitLocY(auth.getQuitLocY());
        cachedAuth.setQuitLocZ(auth.getQuitLocZ());
        cachedAuth.setWorld(auth.getWorld());
        exec.execute(new Runnable() {
            @Override
            public void run() {
                if (!source.updateQuitLoc(auth)) {
                    if (cache.containsKey(auth.getNickname())) {
                        PlayerAuth cachedAuth = cache.get(auth.getNickname());
                        cachedAuth.setQuitLocX(oldX);
                        cachedAuth.setQuitLocY(oldY);
                        cachedAuth.setQuitLocZ(oldZ);
                        cachedAuth.setWorld(oldWorld);
                    }
                }
            }
        });
        return true;
    }

    /**
     * Method getIps.
     *
     * @param ip String
     * @return int * @see fr.xephi.authme.datasource.DataSource#getIps(String)
     */
    @Override
    public int getIps(String ip) {
        int count = 0;
        for (Map.Entry<String, PlayerAuth> p : cache.entrySet()) {
            if (p.getValue().getIp().equals(ip)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Method purgeDatabase.
     *
     * @param until long
     * @return int * @see fr.xephi.authme.datasource.DataSource#purgeDatabase(long)
     */
    @Override
    public int purgeDatabase(long until) {
        int cleared = source.purgeDatabase(until);
        if (cleared > 0) {
            for (PlayerAuth auth : cache.values()) {
                if (auth.getLastLogin() < until) {
                    cache.remove(auth.getNickname());
                }
            }
        }
        return cleared;
    }

    /**
     * Method autoPurgeDatabase.
     *
     * @param until long
     * @return List<String> * @see fr.xephi.authme.datasource.DataSource#autoPurgeDatabase(long)
     */
    @Override
    public List<String> autoPurgeDatabase(long until) {
        List<String> cleared = source.autoPurgeDatabase(until);
        if (cleared.size() > 0) {
            for (PlayerAuth auth : cache.values()) {
                if (auth.getLastLogin() < until) {
                    cache.remove(auth.getNickname());
                }
            }
        }
        return cleared;
    }

    /**
     * Method removeAuth.
     *
     * @param username String
     * @return boolean * @see fr.xephi.authme.datasource.DataSource#removeAuth(String)
     */
    @Override
    public synchronized boolean removeAuth(String username) {
        final String user = username.toLowerCase();
        final PlayerAuth auth = cache.get(user);
        cache.remove(user);
        exec.execute(new Runnable() {
            @Override
            public void run() {
                if (!source.removeAuth(user)) {
                    cache.put(user, auth);
                }
            }
        });
        return true;
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
    public void reload() {
        exec.execute(new Runnable() {
            @Override
            public void run() {
                cache.clear();
                source.reload();
                for (Player player : Utils.getOnlinePlayers()) {
                    String user = player.getName().toLowerCase();
                    if (PlayerCache.getInstance().isAuthenticated(user)) {
                        PlayerAuth auth = source.getAuth(user);
                        cache.put(user, auth);
                    }
                }
            }
        });
    }

    /**
     * Method updateEmail.
     *
     * @param auth PlayerAuth
     * @return boolean * @see fr.xephi.authme.datasource.DataSource#updateEmail(PlayerAuth)
     */
    @Override
    public synchronized boolean updateEmail(final PlayerAuth auth) {
        try {
            return exec.submit(new Callable<Boolean>() {
                public Boolean call() {
                    return source.updateEmail(auth);
                }
            }).get();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Method updateSalt.
     *
     * @param auth PlayerAuth
     * @return boolean * @see fr.xephi.authme.datasource.DataSource#updateSalt(PlayerAuth)
     */
    @Override
    public synchronized boolean updateSalt(final PlayerAuth auth) {
        if (!cache.containsKey(auth.getNickname())) {
            return false;
        }
        PlayerAuth cachedAuth = cache.get(auth.getNickname());
        final String oldSalt = cachedAuth.getSalt();
        cachedAuth.setSalt(auth.getSalt());
        exec.execute(new Runnable() {
            @Override
            public void run() {
                if (!source.updateSalt(auth)) {
                    if (cache.containsKey(auth.getNickname())) {
                        cache.get(auth.getNickname()).setSalt(oldSalt);
                    }
                }
            }
        });
        return true;
    }

    /**
     * Method getAllAuthsByName.
     *
     * @param auth PlayerAuth
     * @return List<String> * @see fr.xephi.authme.datasource.DataSource#getAllAuthsByName(PlayerAuth)
     */
    @Override
    public synchronized List<String> getAllAuthsByName(PlayerAuth auth) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, PlayerAuth> stringPlayerAuthEntry : cache.entrySet()) {
            PlayerAuth p = stringPlayerAuthEntry.getValue();
            if (p.getIp().equals(auth.getIp()))
                result.add(p.getNickname());
        }
        return result;
    }

    /**
     * Method getAllAuthsByIp.
     *
     * @param ip String
     * @return List<String> * @throws Exception * @see fr.xephi.authme.datasource.DataSource#getAllAuthsByIp(String)
     */
    @Override
    public synchronized List<String> getAllAuthsByIp(final String ip) throws Exception {
        return exec.submit(new Callable<List<String>>() {
            public List<String> call() throws Exception {
                return source.getAllAuthsByIp(ip);
            }
        }).get();
    }

    /**
     * Method getAllAuthsByEmail.
     *
     * @param email String
     * @return List<String> * @throws Exception * @see fr.xephi.authme.datasource.DataSource#getAllAuthsByEmail(String)
     */
    @Override
    public synchronized List<String> getAllAuthsByEmail(final String email) throws Exception {
        return exec.submit(new Callable<List<String>>() {
            public List<String> call() throws Exception {
                return source.getAllAuthsByEmail(email);
            }
        }).get();
    }

    /**
     * Method purgeBanned.
     *
     * @param banned List<String>
     * @see fr.xephi.authme.datasource.DataSource#purgeBanned(List<String>)
     */
    @Override
    public synchronized void purgeBanned(final List<String> banned) {
        exec.execute(new Runnable() {
            @Override
            public void run() {
                source.purgeBanned(banned);
                for (PlayerAuth auth : cache.values()) {
                    if (banned.contains(auth.getNickname())) {
                        cache.remove(auth.getNickname());
                    }
                }
            }
        });
    }

    /**
     * Method getType.
     *
     * @return DataSourceType * @see fr.xephi.authme.datasource.DataSource#getType()
     */
    @Override
    public DataSourceType getType() {
        return source.getType();
    }

    /**
     * Method isLogged.
     *
     * @param user String
     * @return boolean * @see fr.xephi.authme.datasource.DataSource#isLogged(String)
     */
    @Override
    public boolean isLogged(String user) {
        user = user.toLowerCase();
        return PlayerCache.getInstance().getCache().containsKey(user);
    }

    /**
     * Method setLogged.
     *
     * @param user String
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
            }
        });
    }

    /**
     * Method getAccountsRegistered.
     *
     * @return int * @see fr.xephi.authme.datasource.DataSource#getAccountsRegistered()
     */
    @Override
    public int getAccountsRegistered() {
        return cache.size();
    }

    /**
     * Method updateName.
     *
     * @param oldone String
     * @param newone String
     * @see fr.xephi.authme.datasource.DataSource#updateName(String, String)
     */
    @Override
    public void updateName(final String oldone, final String newone) {
        if (cache.containsKey(oldone)) {
            cache.put(newone, cache.get(oldone));
            cache.remove(oldone);
        }
        exec.execute(new Runnable() {
            @Override
            public void run() {
                source.updateName(oldone, newone);
            }
        });
    }

    /**
     * Method getAllAuths.
     *
     * @return List<PlayerAuth> * @see fr.xephi.authme.datasource.DataSource#getAllAuths()
     */
    @Override
    public List<PlayerAuth> getAllAuths() {
        return new ArrayList<>(cache.values());
    }

    /**
     * Method getLoggedPlayers.
     *
     * @return List<PlayerAuth> * @see fr.xephi.authme.datasource.DataSource#getLoggedPlayers()
     */
    @Override
    public List<PlayerAuth> getLoggedPlayers() {
        return new ArrayList<>(PlayerCache.getInstance().getCache().values());
    }
}
