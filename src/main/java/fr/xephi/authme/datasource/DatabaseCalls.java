package fr.xephi.authme.datasource;

import fr.xephi.authme.cache.auth.PlayerAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class DatabaseCalls implements DataSource {

    private DataSource database;
    private final ExecutorService exec;

    public DatabaseCalls(DataSource database) {
        this.database = database;
        this.exec = Executors.newCachedThreadPool();
    }

    @Override
    public synchronized boolean isAuthAvailable(final String user) {
        try {
            return exec.submit(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return database.isAuthAvailable(user);
                }
            }).get();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public synchronized PlayerAuth getAuth(final String user) {
        try {
            return exec.submit(new Callable<PlayerAuth>() {
                public PlayerAuth call() throws Exception {
                    return database.getAuth(user);
                }
            }).get();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public synchronized boolean saveAuth(final PlayerAuth auth) {
        try {
            return exec.submit(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return database.saveAuth(auth);
                }
            }).get();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public synchronized boolean updateSession(final PlayerAuth auth) {
        try {
            return exec.submit(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return database.updateSession(auth);
                }
            }).get();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public synchronized boolean updatePassword(final PlayerAuth auth) {
        try {
            return exec.submit(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return database.updatePassword(auth);
                }
            }).get();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public synchronized int purgeDatabase(final long until) {
        try {
            return exec.submit(new Callable<Integer>() {
                public Integer call() throws Exception {
                    return database.purgeDatabase(until);
                }
            }).get();
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public synchronized List<String> autoPurgeDatabase(final long until) {
        try {
            return exec.submit(new Callable<List<String>>() {
                public List<String> call() throws Exception {
                    return database.autoPurgeDatabase(until);
                }
            }).get();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public synchronized boolean removeAuth(final String user) {
        try {
            return exec.submit(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return database.removeAuth(user);
                }
            }).get();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public synchronized boolean updateQuitLoc(final PlayerAuth auth) {
        try {
            return exec.submit(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return database.updateQuitLoc(auth);
                }
            }).get();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public synchronized int getIps(final String ip) {
        try {
            return exec.submit(new Callable<Integer>() {

                public Integer call() throws Exception {
                    return database.getIps(ip);
                }
            }).get();
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public synchronized List<String> getAllAuthsByName(final PlayerAuth auth) {
        try {
            return exec.submit(new Callable<List<String>>() {
                public List<String> call() throws Exception {
                    return database.getAllAuthsByName(auth);
                }
            }).get();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public synchronized List<String> getAllAuthsByIp(final String ip) {
        try {
            return exec.submit(new Callable<List<String>>() {
                public List<String> call() throws Exception {
                    return database.getAllAuthsByIp(ip);
                }
            }).get();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public synchronized List<String> getAllAuthsByEmail(final String email) {
        try {
            return exec.submit(new Callable<List<String>>() {
                public List<String> call() throws Exception {
                    return database.getAllAuthsByEmail(email);
                }
            }).get();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public synchronized boolean updateEmail(final PlayerAuth auth) {
        try {
            return exec.submit(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return database.updateEmail(auth);
                }
            }).get();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public synchronized boolean updateSalt(final PlayerAuth auth) {
        try {
            return exec.submit(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return database.updateSalt(auth);
                }
            }).get();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public synchronized void close() {
        try {
            exec.shutdown();
            exec.awaitTermination(10, TimeUnit.SECONDS);
            database.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void reload() {
        database.reload();
    }

    @Override
    public synchronized void purgeBanned(final List<String> banned) {
        new Thread(new Runnable() {
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
        try {
            return exec.submit(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return database.isLogged(user);
                }
            }).get();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public synchronized void setLogged(final String user) {
        exec.execute(new Runnable() {
            public synchronized void run() {
                database.setLogged(user);
            }
        });
    }

    @Override
    public synchronized void setUnlogged(final String user) {
        exec.execute(new Runnable() {
            public synchronized void run() {
                database.setUnlogged(user);
            }
        });
    }

    @Override
    public synchronized void purgeLogged() {
        exec.execute(new Runnable() {
            public synchronized void run() {
                database.purgeLogged();
            }
        });
    }

    @Override
    public synchronized int getAccountsRegistered() {
        try {
            return exec.submit(new Callable<Integer>() {
                public Integer call() throws Exception {
                    return database.getAccountsRegistered();
                }
            }).get();
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public synchronized void updateName(final String oldone, final String newone) {
        exec.execute(new Runnable() {
            public synchronized void run() {
                database.updateName(oldone, newone);
            }
        });
    }

    @Override
    public synchronized List<PlayerAuth> getAllAuths() {
        try {
            return exec.submit(new Callable<List<PlayerAuth>>() {
                public List<PlayerAuth> call() throws Exception {
                    return database.getAllAuths();
                }
            }).get();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public List<PlayerAuth> getLoggedPlayers() {
        try {
            return exec.submit(new Callable<List<PlayerAuth>>() {
                public List<PlayerAuth> call() throws Exception {
                    return database.getLoggedPlayers();
                }
            }).get();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

}
