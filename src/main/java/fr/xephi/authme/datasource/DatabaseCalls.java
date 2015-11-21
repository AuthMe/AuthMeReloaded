package fr.xephi.authme.datasource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.xephi.authme.cache.auth.PlayerAuth;

/**
 */
public class DatabaseCalls implements DataSource {

    private DataSource database;
    private final ExecutorService exec;

    /**
     * Constructor for DatabaseCalls.
     * @param database DataSource
     */
    public DatabaseCalls(DataSource database) {
        this.database = database;
        this.exec = Executors.newCachedThreadPool();
    }

    /**
     * Method isAuthAvailable.
     * @param user String
     * @return boolean
     * @see fr.xephi.authme.datasource.DataSource#isAuthAvailable(String)
     */
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

    /**
     * Method getAuth.
     * @param user String
     * @return PlayerAuth
     * @see fr.xephi.authme.datasource.DataSource#getAuth(String)
     */
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

    /**
     * Method saveAuth.
     * @param auth PlayerAuth
     * @return boolean
     * @see fr.xephi.authme.datasource.DataSource#saveAuth(PlayerAuth)
     */
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

    /**
     * Method updateSession.
     * @param auth PlayerAuth
     * @return boolean
     * @see fr.xephi.authme.datasource.DataSource#updateSession(PlayerAuth)
     */
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

    /**
     * Method updatePassword.
     * @param auth PlayerAuth
     * @return boolean
     * @see fr.xephi.authme.datasource.DataSource#updatePassword(PlayerAuth)
     */
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

    /**
     * Method purgeDatabase.
     * @param until long
     * @return int
     * @see fr.xephi.authme.datasource.DataSource#purgeDatabase(long)
     */
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

    /**
     * Method autoPurgeDatabase.
     * @param until long
     * @return List<String>
     * @see fr.xephi.authme.datasource.DataSource#autoPurgeDatabase(long)
     */
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

    /**
     * Method removeAuth.
     * @param user String
     * @return boolean
     * @see fr.xephi.authme.datasource.DataSource#removeAuth(String)
     */
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

    /**
     * Method updateQuitLoc.
     * @param auth PlayerAuth
     * @return boolean
     * @see fr.xephi.authme.datasource.DataSource#updateQuitLoc(PlayerAuth)
     */
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

    /**
     * Method getIps.
     * @param ip String
     * @return int
     * @see fr.xephi.authme.datasource.DataSource#getIps(String)
     */
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

    /**
     * Method getAllAuthsByName.
     * @param auth PlayerAuth
     * @return List<String>
     * @see fr.xephi.authme.datasource.DataSource#getAllAuthsByName(PlayerAuth)
     */
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

    /**
     * Method getAllAuthsByIp.
     * @param ip String
     * @return List<String>
     * @see fr.xephi.authme.datasource.DataSource#getAllAuthsByIp(String)
     */
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

    /**
     * Method getAllAuthsByEmail.
     * @param email String
     * @return List<String>
     * @see fr.xephi.authme.datasource.DataSource#getAllAuthsByEmail(String)
     */
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

    /**
     * Method updateEmail.
     * @param auth PlayerAuth
     * @return boolean
     * @see fr.xephi.authme.datasource.DataSource#updateEmail(PlayerAuth)
     */
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

    /**
     * Method updateSalt.
     * @param auth PlayerAuth
     * @return boolean
     * @see fr.xephi.authme.datasource.DataSource#updateSalt(PlayerAuth)
     */
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

    /**
     * Method close.
     * @see fr.xephi.authme.datasource.DataSource#close()
     */
    @Override
    public synchronized void close() {
        exec.shutdown();
        database.close();
    }

    /**
     * Method reload.
     * @see fr.xephi.authme.datasource.DataSource#reload()
     */
    @Override
    public synchronized void reload() {
        database.reload();
    }

    /**
     * Method purgeBanned.
     * @param banned List<String>
     * @see fr.xephi.authme.datasource.DataSource#purgeBanned(List<String>)
     */
    @Override
    public synchronized void purgeBanned(final List<String> banned) {
        new Thread(new Runnable() {
            public synchronized void run() {
                database.purgeBanned(banned);
            }
        }).start();
    }

    /**
     * Method getType.
     * @return DataSourceType
     * @see fr.xephi.authme.datasource.DataSource#getType()
     */
    @Override
    public synchronized DataSourceType getType() {
        return database.getType();
    }

    /**
     * Method isLogged.
     * @param user String
     * @return boolean
     * @see fr.xephi.authme.datasource.DataSource#isLogged(String)
     */
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

    /**
     * Method setLogged.
     * @param user String
     * @see fr.xephi.authme.datasource.DataSource#setLogged(String)
     */
    @Override
    public synchronized void setLogged(final String user) {
        exec.execute(new Runnable() {
            public synchronized void run() {
                database.setLogged(user);
            }
        });
    }

    /**
     * Method setUnlogged.
     * @param user String
     * @see fr.xephi.authme.datasource.DataSource#setUnlogged(String)
     */
    @Override
    public synchronized void setUnlogged(final String user) {
        exec.execute(new Runnable() {
            public synchronized void run() {
                database.setUnlogged(user);
            }
        });
    }

    /**
     * Method purgeLogged.
     * @see fr.xephi.authme.datasource.DataSource#purgeLogged()
     */
    @Override
    public synchronized void purgeLogged() {
        exec.execute(new Runnable() {
            public synchronized void run() {
                database.purgeLogged();
            }
        });
    }

    /**
     * Method getAccountsRegistered.
     * @return int
     * @see fr.xephi.authme.datasource.DataSource#getAccountsRegistered()
     */
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

    /**
     * Method updateName.
     * @param oldone String
     * @param newone String
     * @see fr.xephi.authme.datasource.DataSource#updateName(String, String)
     */
    @Override
    public synchronized void updateName(final String oldone, final String newone) {
        exec.execute(new Runnable() {
            public synchronized void run() {
                database.updateName(oldone, newone);
            }
        });
    }

    /**
     * Method getAllAuths.
     * @return List<PlayerAuth>
     * @see fr.xephi.authme.datasource.DataSource#getAllAuths()
     */
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

    /**
     * Method getLoggedPlayers.
     * @return List<PlayerAuth>
     * @see fr.xephi.authme.datasource.DataSource#getLoggedPlayers()
     */
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
