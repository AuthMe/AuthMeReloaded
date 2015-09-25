package fr.xephi.authme.datasource;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.Utils;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CacheDataSource implements DataSource {

    private final DataSource source;
    private final AuthMe plugin;
    private final ExecutorService exec;
    private final ConcurrentHashMap<String, PlayerAuth> cache = new ConcurrentHashMap<>();

    public CacheDataSource(AuthMe pl, DataSource src) {
        this.plugin = pl;
        this.source = src;
        this.exec = Executors.newCachedThreadPool();

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
            }
        });
    }

    @Override
    public synchronized boolean isAuthAvailable(String user) {
        return cache.containsKey(user.toLowerCase());
    }

    @Override
    public synchronized PlayerAuth getAuth(String user) {
        user = user.toLowerCase();
        if (cache.containsKey(user)) {
            return cache.get(user);
        }
        return null;
    }

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

    @Override
    public synchronized void close() {
        exec.shutdown();
        source.close();
    }

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

    @Override
    public synchronized boolean updateEmail(final PlayerAuth auth) {
        if (!cache.containsKey(auth.getNickname())) {
            return false;
        }
        PlayerAuth cachedAuth = cache.get(auth.getNickname());
        final String oldEmail = cachedAuth.getEmail();
        cachedAuth.setEmail(auth.getEmail());
        exec.execute(new Runnable() {
            @Override
            public void run() {
                if (!source.updateEmail(auth)) {
                    if (cache.containsKey(auth.getNickname())) {
                        cache.get(auth.getNickname()).setEmail(oldEmail);
                    }
                }
            }
        });
        return true;
    }

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

    @Override
    public synchronized List<String> getAllAuthsByIp(String ip) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, PlayerAuth> stringPlayerAuthEntry : cache.entrySet()) {
            PlayerAuth p = stringPlayerAuthEntry.getValue();
            if (p.getIp().equals(ip))
                result.add(p.getNickname());
        }
        return result;
    }

    @Override
    public synchronized List<String> getAllAuthsByEmail(String email) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, PlayerAuth> stringPlayerAuthEntry : cache.entrySet()) {
            PlayerAuth p = stringPlayerAuthEntry.getValue();
            if (p.getEmail().equals(email))
                result.add(p.getNickname());
        }
        return result;
    }

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

    @Override
    public DataSourceType getType() {
        return source.getType();
    }

    @Override
    public boolean isLogged(String user) {
        user = user.toLowerCase();
        return PlayerCache.getInstance().getCache().containsKey(user);
    }

    @Override
    public void setLogged(final String user) {
        exec.execute(new Runnable() {
            @Override
            public void run() {
                source.setLogged(user.toLowerCase());
            }
        });
    }

    @Override
    public void setUnlogged(final String user) {
        exec.execute(new Runnable() {
            @Override
            public void run() {
                source.setUnlogged(user.toLowerCase());
            }
        });
    }

    @Override
    public void purgeLogged() {
        exec.execute(new Runnable() {
            @Override
            public void run() {
                source.purgeLogged();
            }
        });
    }

    @Override
    public int getAccountsRegistered() {
        return cache.size();
    }

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

    @Override
    public List<PlayerAuth> getAllAuths() {
        return new ArrayList<>(cache.values());
    }

    @Override
    public List<PlayerAuth> getLoggedPlayers() {
        return new ArrayList<>(PlayerCache.getInstance().getCache().values());
    }
}
