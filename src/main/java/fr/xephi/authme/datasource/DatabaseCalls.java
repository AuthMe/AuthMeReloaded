package fr.xephi.authme.datasource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.xephi.authme.cache.auth.PlayerAuth;

public class DatabaseCalls implements DataSource {

    private DataSource database;

    public DatabaseCalls(DataSource database) {
        this.database = database;
    }

    @Override
    public synchronized boolean isAuthAvailable(final String user) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Boolean result;
        try {
            result = executor.submit(new Callable<Boolean>() {

                public Boolean call() throws Exception {
                    return database.isAuthAvailable(user);
                }
            }).get();
        } catch (InterruptedException e1) {
            return false;
        } catch (ExecutionException e1) {
            return false;
        } finally {
            executor.shutdown();
        }
        try {
            return result.booleanValue();
        } catch (Exception e) {
            return (false);
        }
    }

    @Override
    public synchronized PlayerAuth getAuth(final String user) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        PlayerAuth result;
        try {
            result = executor.submit(new Callable<PlayerAuth>() {

                public PlayerAuth call() throws Exception {
                    return database.getAuth(user);
                }
            }).get();
        } catch (InterruptedException e1) {
            return null;
        } catch (ExecutionException e1) {
            return null;
        } finally {
            executor.shutdown();
        }
        try {
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public synchronized boolean saveAuth(final PlayerAuth auth) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Boolean result;
        try {
            result = executor.submit(new Callable<Boolean>() {

                public Boolean call() throws Exception {
                    return database.saveAuth(auth);
                }
            }).get();
        } catch (InterruptedException e1) {
            return false;
        } catch (ExecutionException e1) {
            return false;
        } finally {
            executor.shutdown();
        }
        try {
            return result.booleanValue();
        } catch (Exception e) {
            return (false);
        }
    }

    @Override
    public synchronized boolean updateSession(final PlayerAuth auth) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Boolean result;
        try {
            result = executor.submit(new Callable<Boolean>() {

                public Boolean call() throws Exception {
                    return database.updateSession(auth);
                }
            }).get();
        } catch (InterruptedException e1) {
            return false;
        } catch (ExecutionException e1) {
            return false;
        } finally {
            executor.shutdown();
        }
        try {
            return result.booleanValue();
        } catch (Exception e) {
            return (false);
        }
    }

    @Override
    public synchronized boolean updatePassword(final PlayerAuth auth) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Boolean result;
        try {
            result = executor.submit(new Callable<Boolean>() {

                public Boolean call() throws Exception {
                    return database.updatePassword(auth);
                }
            }).get();
        } catch (InterruptedException e1) {
            return false;
        } catch (ExecutionException e1) {
            return false;
        } finally {
            executor.shutdown();
        }
        try {
            return result.booleanValue();
        } catch (Exception e) {
            return (false);
        }
    }

    @Override
    public synchronized int purgeDatabase(final long until) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Integer result;
        try {
            result = executor.submit(new Callable<Integer>() {

                public Integer call() throws Exception {
                    return database.purgeDatabase(until);
                }
            }).get();
        } catch (InterruptedException e1) {
            return 0;
        } catch (ExecutionException e1) {
            return 0;
        } finally {
            executor.shutdown();
        }
        try {
            return result.intValue();
        } catch (Exception e) {
            return (0);
        }
    }

    @Override
    public synchronized List<String> autoPurgeDatabase(final long until) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        List<String> result;
        try {
            result = executor.submit(new Callable<List<String>>() {

                public List<String> call() throws Exception {
                    return database.autoPurgeDatabase(until);
                }
            }).get();
        } catch (InterruptedException e1) {
            return new ArrayList<String>();
        } catch (ExecutionException e1) {
            return new ArrayList<String>();
        } finally {
            executor.shutdown();
        }
        try {
            return result;
        } catch (Exception e) {
            return (new ArrayList<String>());
        }
    }

    @Override
    public synchronized boolean removeAuth(final String user) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Boolean result;
        try {
            result = executor.submit(new Callable<Boolean>() {

                public Boolean call() throws Exception {
                    return database.removeAuth(user);
                }
            }).get();
        } catch (InterruptedException e1) {
            return false;
        } catch (ExecutionException e1) {
            return false;
        } finally {
            executor.shutdown();
        }
        try {
            return result.booleanValue();
        } catch (Exception e) {
            return (false);
        }
    }

    @Override
    public synchronized boolean updateQuitLoc(final PlayerAuth auth) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Boolean result;
        try {
            result = executor.submit(new Callable<Boolean>() {

                public Boolean call() throws Exception {
                    return database.updateQuitLoc(auth);
                }
            }).get();
        } catch (InterruptedException e1) {
            return false;
        } catch (ExecutionException e1) {
            return false;
        } finally {
            executor.shutdown();
        }
        try {
            return result.booleanValue();
        } catch (Exception e) {
            return (false);
        }
    }

    @Override
    public synchronized int getIps(final String ip) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Integer result;
        try {
            result = executor.submit(new Callable<Integer>() {

                public Integer call() throws Exception {
                    return database.getIps(ip);
                }
            }).get();
        } catch (InterruptedException e1) {
            return 0;
        } catch (ExecutionException e1) {
            return 0;
        } finally {
            executor.shutdown();
        }
        try {
            return result.intValue();
        } catch (Exception e) {
            return (0);
        }
    }

    @Override
    public synchronized List<String> getAllAuthsByName(final PlayerAuth auth) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        List<String> result;
        try {
            result = executor.submit(new Callable<List<String>>() {

                public List<String> call() throws Exception {
                    return database.getAllAuthsByName(auth);
                }
            }).get();
        } catch (InterruptedException e1) {
            return new ArrayList<String>();
        } catch (ExecutionException e1) {
            return new ArrayList<String>();
        } finally {
            executor.shutdown();
        }
        try {
            return result;
        } catch (Exception e) {
            return (new ArrayList<String>());
        }
    }

    @Override
    public synchronized List<String> getAllAuthsByIp(final String ip) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        List<String> result;
        try {
            result = executor.submit(new Callable<List<String>>() {

                public List<String> call() throws Exception {
                    return database.getAllAuthsByIp(ip);
                }
            }).get();
        } catch (InterruptedException e1) {
            return new ArrayList<String>();
        } catch (ExecutionException e1) {
            return new ArrayList<String>();
        } finally {
            executor.shutdown();
        }
        try {
            return result;
        } catch (Exception e) {
            return (new ArrayList<String>());
        }
    }

    @Override
    public synchronized List<String> getAllAuthsByEmail(final String email) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        List<String> result;
        try {
            result = executor.submit(new Callable<List<String>>() {

                public List<String> call() throws Exception {
                    return database.getAllAuthsByEmail(email);
                }
            }).get();
        } catch (InterruptedException e1) {
            return new ArrayList<String>();
        } catch (ExecutionException e1) {
            return new ArrayList<String>();
        } finally {
            executor.shutdown();
        }
        try {
            return result;
        } catch (Exception e) {
            return (new ArrayList<String>());
        }
    }

    @Override
    public synchronized boolean updateEmail(final PlayerAuth auth) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Boolean result;
        try {
            result = executor.submit(new Callable<Boolean>() {

                public Boolean call() throws Exception {
                    return database.updateEmail(auth);
                }
            }).get();
        } catch (InterruptedException e1) {
            return false;
        } catch (ExecutionException e1) {
            return false;
        } finally {
            executor.shutdown();
        }
        try {
            return result.booleanValue();
        } catch (Exception e) {
            return (false);
        }
    }

    @Override
    public synchronized boolean updateSalt(final PlayerAuth auth) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Boolean result;
        try {
            result = executor.submit(new Callable<Boolean>() {

                public Boolean call() throws Exception {
                    return database.updateSalt(auth);
                }
            }).get();
        } catch (InterruptedException e1) {
            return false;
        } catch (ExecutionException e1) {
            return false;
        } finally {
            executor.shutdown();
        }
        try {
            return result.booleanValue();
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
        Boolean result;
        try {
            result = executor.submit(new Callable<Boolean>() {

                public Boolean call() throws Exception {
                    return database.isLogged(user);
                }
            }).get();
        } catch (InterruptedException e1) {
            return false;
        } catch (ExecutionException e1) {
            return false;
        } finally {
            executor.shutdown();
        }
        try {
            return result.booleanValue();
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
        Integer result;
        try {
            result = executor.submit(new Callable<Integer>() {

                public Integer call() throws Exception {
                    return database.getAccountsRegistered();
                }
            }).get();
        } catch (InterruptedException e1) {
            return 0;
        } catch (ExecutionException e1) {
            return 0;
        } finally {
            executor.shutdown();
        }
        try {
            return result.intValue();
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
        List<PlayerAuth> result;
        try {
            result = executor.submit(new Callable<List<PlayerAuth>>() {

                public List<PlayerAuth> call() throws Exception {
                    return database.getAllAuths();
                }
            }).get();
        } catch (InterruptedException e1) {
            return (new ArrayList<PlayerAuth>());
        } catch (ExecutionException e1) {
            return (new ArrayList<PlayerAuth>());
        } finally {
            executor.shutdown();
        }
        return result;
    }

    @Override
    public List<PlayerAuth> getLoggedPlayers() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        List<PlayerAuth> result;
        try {
            result = executor.submit(new Callable<List<PlayerAuth>>() {

                public List<PlayerAuth> call() throws Exception {
                    return database.getLoggedPlayers();
                }
            }).get();
        } catch (InterruptedException e1) {
            return (new ArrayList<PlayerAuth>());
        } catch (ExecutionException e1) {
            return (new ArrayList<PlayerAuth>());
        } finally {
            executor.shutdown();
        }
        return result;
    }

}
