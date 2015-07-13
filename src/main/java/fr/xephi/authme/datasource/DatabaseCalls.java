package fr.xephi.authme.datasource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import fr.xephi.authme.cache.auth.PlayerAuth;

public class DatabaseCalls implements DataSource {

    private DataSource database;

    public DatabaseCalls(DataSource database) {
        this.database = database;
    }

    @Override
    public synchronized boolean isAuthAvailable(final String user) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> result = executor.submit(new Callable<Boolean>() {

            public Boolean call() throws Exception {
                return database.isAuthAvailable(user);
            }
        });
        try {
            return result.get();
        } catch (Exception e) {
            return (false);
        }
    }

    @Override
    public synchronized PlayerAuth getAuth(final String user) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<PlayerAuth> result = executor.submit(new Callable<PlayerAuth>() {

            public PlayerAuth call() throws Exception {
                return database.getAuth(user);
            }
        });
        try {
            return result.get();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public synchronized boolean saveAuth(final PlayerAuth auth) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> result = executor.submit(new Callable<Boolean>() {

            public Boolean call() throws Exception {
                return database.saveAuth(auth);
            }
        });
        try {
            return result.get();
        } catch (Exception e) {
            return (false);
        }
    }

    @Override
    public synchronized boolean updateSession(final PlayerAuth auth) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> result = executor.submit(new Callable<Boolean>() {

            public Boolean call() throws Exception {
                return database.updateSession(auth);
            }
        });
        try {
            return result.get();
        } catch (Exception e) {
            return (false);
        }
    }

    @Override
    public synchronized boolean updatePassword(final PlayerAuth auth) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> result = executor.submit(new Callable<Boolean>() {

            public Boolean call() throws Exception {
                return database.updatePassword(auth);
            }
        });
        try {
            return result.get();
        } catch (Exception e) {
            return (false);
        }
    }

    @Override
    public synchronized int purgeDatabase(final long until) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Integer> result = executor.submit(new Callable<Integer>() {

            public Integer call() throws Exception {
                return database.purgeDatabase(until);
            }
        });
        try {
            return result.get();
        } catch (Exception e) {
            return (0);
        }
    }

    @Override
    public synchronized List<String> autoPurgeDatabase(final long until) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<List<String>> result = executor.submit(new Callable<List<String>>() {

            public List<String> call() throws Exception {
                return database.autoPurgeDatabase(until);
            }
        });
        try {
            return result.get();
        } catch (Exception e) {
            return (new ArrayList<String>());
        }
    }

    @Override
    public synchronized boolean removeAuth(final String user) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> result = executor.submit(new Callable<Boolean>() {

            public Boolean call() throws Exception {
                return database.removeAuth(user);
            }
        });
        try {
            return result.get();
        } catch (Exception e) {
            return (false);
        }
    }

    @Override
    public synchronized boolean updateQuitLoc(final PlayerAuth auth) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> result = executor.submit(new Callable<Boolean>() {

            public Boolean call() throws Exception {
                return database.updateQuitLoc(auth);
            }
        });
        try {
            return result.get();
        } catch (Exception e) {
            return (false);
        }
    }

    @Override
    public synchronized int getIps(final String ip) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Integer> result = executor.submit(new Callable<Integer>() {

            public Integer call() throws Exception {
                return database.getIps(ip);
            }
        });
        try {
            return result.get();
        } catch (Exception e) {
            return (0);
        }
    }

    @Override
    public synchronized List<String> getAllAuthsByName(final PlayerAuth auth) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<List<String>> result = executor.submit(new Callable<List<String>>() {

            public List<String> call() throws Exception {
                return database.getAllAuthsByName(auth);
            }
        });
        try {
            return result.get();
        } catch (Exception e) {
            return (new ArrayList<String>());
        }
    }

    @Override
    public synchronized List<String> getAllAuthsByIp(final String ip) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<List<String>> result = executor.submit(new Callable<List<String>>() {

            public List<String> call() throws Exception {
                return database.getAllAuthsByIp(ip);
            }
        });
        try {
            return result.get();
        } catch (Exception e) {
            return (new ArrayList<String>());
        }
    }

    @Override
    public synchronized List<String> getAllAuthsByEmail(final String email) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<List<String>> result = executor.submit(new Callable<List<String>>() {

            public List<String> call() throws Exception {
                return database.getAllAuthsByEmail(email);
            }
        });
        try {
            return result.get();
        } catch (Exception e) {
            return (new ArrayList<String>());
        }
    }

    @Override
    public synchronized boolean updateEmail(final PlayerAuth auth) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> result = executor.submit(new Callable<Boolean>() {

            public Boolean call() throws Exception {
                return database.updateEmail(auth);
            }
        });
        try {
            return result.get();
        } catch (Exception e) {
            return (false);
        }
    }

    @Override
    public synchronized boolean updateSalt(final PlayerAuth auth) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> result = executor.submit(new Callable<Boolean>() {

            public Boolean call() throws Exception {
                return database.updateSalt(auth);
            }
        });
        try {
            return result.get();
        } catch (Exception e) {
            return (false);
        }
    }

    @Override
    public synchronized void close() {
        database.close();
    }

    @Override
    public synchronized void reload() {
        database.reload();
    }

    @Override
    public synchronized void purgeBanned(final List<String> banned) {
        new Thread(new Runnable() {

            @Override
            public synchronized void run() {
                database.purgeBanned(banned);
            }
        }).start();
    }

    @Override
    public synchronized DataSourceType getType() {
        return database.getType();
    }

    @Override
    public synchronized boolean isLogged(final String user) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> result = executor.submit(new Callable<Boolean>() {

            public Boolean call() throws Exception {
                return database.isLogged(user);
            }
        });
        try {
            return result.get();
        } catch (Exception e) {
            return (false);
        }
    }

    @Override
    public synchronized void setLogged(final String user) {
        new Thread(new Runnable() {

            @Override
            public synchronized void run() {
                database.setLogged(user);
            }
        }).start();
    }

    @Override
    public synchronized void setUnlogged(final String user) {
        new Thread(new Runnable() {

            @Override
            public synchronized void run() {
                database.setUnlogged(user);
            }
        }).start();
    }

    @Override
    public synchronized void purgeLogged() {
        new Thread(new Runnable() {

            @Override
            public synchronized void run() {
                database.purgeLogged();
            }
        }).start();
    }

    @Override
    public synchronized int getAccountsRegistered() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Integer> result = executor.submit(new Callable<Integer>() {

            public Integer call() throws Exception {
                return database.getAccountsRegistered();
            }
        });
        try {
            return result.get();
        } catch (Exception e) {
            return (0);
        }
    }

    @Override
    public synchronized void updateName(final String oldone,
            final String newone) {
        new Thread(new Runnable() {

            @Override
            public synchronized void run() {
                database.updateName(oldone, newone);
            }
        }).start();
    }

    @Override
    public synchronized List<PlayerAuth> getAllAuths() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<List<PlayerAuth>> result = executor.submit(new Callable<List<PlayerAuth>>() {

            public List<PlayerAuth> call() throws Exception {
                return database.getAllAuths();
            }
        });
        try {
            return result.get();
        } catch (Exception e) {
            return (new ArrayList<PlayerAuth>());
        }
    }

}
