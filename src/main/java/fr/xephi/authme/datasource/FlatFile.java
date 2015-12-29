package fr.xephi.authme.datasource;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.settings.Settings;

/**
 */
@Deprecated
public class FlatFile implements DataSource {

    /*
     * file layout:
     *
     * PLAYERNAME:HASHSUM:IP:LOGININMILLIESECONDS:LASTPOSX:LASTPOSY:LASTPOSZ:
     * LASTPOSWORLD:EMAIL
     *
     * Old but compatible:
     * PLAYERNAME:HASHSUM:IP:LOGININMILLIESECONDS:LASTPOSX:LASTPOSY
     * :LASTPOSZ:LASTPOSWORLD PLAYERNAME:HASHSUM:IP:LOGININMILLIESECONDS
     * PLAYERNAME:HASHSUM:IP PLAYERNAME:HASHSUM
     */
    private final File source;

    public FlatFile() {
        source = Settings.AUTH_FILE;
        try {
            source.createNewFile();
        } catch (IOException e) {
            ConsoleLogger.showError(e.getMessage());
            if (Settings.isStopEnabled) {
                ConsoleLogger.showError("Can't use FLAT FILE... SHUTDOWN...");
                AuthMe.getInstance().getServer().shutdown();
            }
            if (!Settings.isStopEnabled) {
                AuthMe.getInstance().getServer().getPluginManager().disablePlugin(AuthMe.getInstance());
            }
            e.printStackTrace();
        }
    }

    /**
     * Method isAuthAvailable.
     *
     * @param user String
     *
     * @return boolean * @see fr.xephi.authme.datasource.DataSource#isAuthAvailable(String)
     */
    @Override
    public synchronized boolean isAuthAvailable(String user) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(source));
            String line;
            while ((line = br.readLine()) != null) {
                String[] args = line.split(":");
                if (args.length > 1 && args[0].equalsIgnoreCase(user)) {
                    return true;
                }
            }
        } catch (FileNotFoundException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } catch (IOException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
        }
        return false;
    }

    /**
     * Method saveAuth.
     *
     * @param auth PlayerAuth
     *
     * @return boolean * @see fr.xephi.authme.datasource.DataSource#saveAuth(PlayerAuth)
     */
    @Override
    public synchronized boolean saveAuth(PlayerAuth auth) {
        if (isAuthAvailable(auth.getNickname())) {
            return false;
        }
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(source, true));
            bw.write(auth.getNickname() + ":" + auth.getHash() + ":" + auth.getIp() + ":" + auth.getLastLogin() + ":" + auth.getQuitLocX() + ":" + auth.getQuitLocY() + ":" + auth.getQuitLocZ() + ":" + auth.getWorld() + ":" + auth.getEmail() + "\n");
        } catch (IOException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException ignored) {
                }
            }
        }
        return true;
    }

    /**
     * Method updatePassword.
     *
     * @param auth PlayerAuth
     *
     * @return boolean * @see fr.xephi.authme.datasource.DataSource#updatePassword(PlayerAuth)
     */
    @Override
    public synchronized boolean updatePassword(PlayerAuth auth) {
        if (!isAuthAvailable(auth.getNickname())) {
            return false;
        }
        PlayerAuth newAuth = null;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(source));
            String line;
            while ((line = br.readLine()) != null) {
                String[] args = line.split(":");
                if (args[0].equals(auth.getNickname())) {
                    switch (args.length) {
                        case 4: {
                            newAuth = new PlayerAuth(args[0], auth.getHash(), args[2], Long.parseLong(args[3]), 0, 0, 0, "world", "your@email.com", args[0]);
                            break;
                        }
                        case 7: {
                            newAuth = new PlayerAuth(args[0], auth.getHash(), args[2], Long.parseLong(args[3]), Double.parseDouble(args[4]), Double.parseDouble(args[5]), Double.parseDouble(args[6]), "world", "your@email.com", args[0]);
                            break;
                        }
                        case 8: {
                            newAuth = new PlayerAuth(args[0], auth.getHash(), args[2], Long.parseLong(args[3]), Double.parseDouble(args[4]), Double.parseDouble(args[5]), Double.parseDouble(args[6]), args[7], "your@email.com", args[0]);
                            break;
                        }
                        case 9: {
                            newAuth = new PlayerAuth(args[0], auth.getHash(), args[2], Long.parseLong(args[3]), Double.parseDouble(args[4]), Double.parseDouble(args[5]), Double.parseDouble(args[6]), args[7], args[8], args[0]);
                            break;
                        }
                        default: {
                            newAuth = new PlayerAuth(args[0], auth.getHash(), args[2], 0, 0, 0, 0, "world", "your@email.com", args[0]);
                            break;
                        }
                    }
                    break;
                }
            }
        } catch (FileNotFoundException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } catch (IOException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
        }
        if (newAuth != null) {
            removeAuth(auth.getNickname());
            saveAuth(newAuth);
        }
        return true;
    }

    /**
     * Method updateSession.
     *
     * @param auth PlayerAuth
     *
     * @return boolean * @see fr.xephi.authme.datasource.DataSource#updateSession(PlayerAuth)
     */
    @Override
    public boolean updateSession(PlayerAuth auth) {
        if (!isAuthAvailable(auth.getNickname())) {
            return false;
        }
        PlayerAuth newAuth = null;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(source));
            String line;
            while ((line = br.readLine()) != null) {
                String[] args = line.split(":");
                if (args[0].equalsIgnoreCase(auth.getNickname())) {
                    switch (args.length) {
                        case 4: {
                            newAuth = new PlayerAuth(args[0], args[1], auth.getIp(), auth.getLastLogin(), 0, 0, 0, "world", "your@email.com", args[0]);
                            break;
                        }
                        case 7: {
                            newAuth = new PlayerAuth(args[0], args[1], auth.getIp(), auth.getLastLogin(), Double.parseDouble(args[4]), Double.parseDouble(args[5]), Double.parseDouble(args[6]), "world", "your@email.com", args[0]);
                            break;
                        }
                        case 8: {
                            newAuth = new PlayerAuth(args[0], args[1], auth.getIp(), auth.getLastLogin(), Double.parseDouble(args[4]), Double.parseDouble(args[5]), Double.parseDouble(args[6]), args[7], "your@email.com", args[0]);
                            break;
                        }
                        case 9: {
                            newAuth = new PlayerAuth(args[0], args[1], auth.getIp(), auth.getLastLogin(), Double.parseDouble(args[4]), Double.parseDouble(args[5]), Double.parseDouble(args[6]), args[7], args[8], args[0]);
                            break;
                        }
                        default: {
                            newAuth = new PlayerAuth(args[0], args[1], auth.getIp(), auth.getLastLogin(), 0, 0, 0, "world", "your@email.com", args[0]);
                            break;
                        }
                    }
                    break;
                }
            }
        } catch (FileNotFoundException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } catch (IOException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
        }
        if (newAuth != null) {
            removeAuth(auth.getNickname());
            saveAuth(newAuth);
        }
        return true;
    }

    /**
     * Method updateQuitLoc.
     *
     * @param auth PlayerAuth
     *
     * @return boolean * @see fr.xephi.authme.datasource.DataSource#updateQuitLoc(PlayerAuth)
     */
    @Override
    public boolean updateQuitLoc(PlayerAuth auth) {
        if (!isAuthAvailable(auth.getNickname())) {
            return false;
        }
        PlayerAuth newAuth = null;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(source));
            String line;
            while ((line = br.readLine()) != null) {
                String[] args = line.split(":");
                if (args[0].equalsIgnoreCase(auth.getNickname())) {
                    newAuth = new PlayerAuth(args[0], args[1], args[2], Long.parseLong(args[3]), auth.getQuitLocX(), auth.getQuitLocY(), auth.getQuitLocZ(), auth.getWorld(), auth.getEmail(), args[0]);
                    break;
                }
            }
        } catch (FileNotFoundException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } catch (IOException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
        }
        if (newAuth != null) {
            removeAuth(auth.getNickname());
            saveAuth(newAuth);
        }
        return true;
    }

    /**
     * Method getIps.
     *
     * @param ip String
     *
     * @return int * @see fr.xephi.authme.datasource.DataSource#getIps(String)
     */
    @Override
    public int getIps(String ip) {
        BufferedReader br = null;
        int countIp = 0;
        try {
            br = new BufferedReader(new FileReader(source));
            String line;
            while ((line = br.readLine()) != null) {
                String[] args = line.split(":");
                if (args.length > 3 && args[2].equals(ip)) {
                    countIp++;
                }
            }
            return countIp;
        } catch (FileNotFoundException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return 0;
        } catch (IOException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return 0;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Method purgeDatabase.
     *
     * @param until long
     *
     * @return int * @see fr.xephi.authme.datasource.DataSource#purgeDatabase(long)
     */
    @Override
    public int purgeDatabase(long until) {
        BufferedReader br = null;
        BufferedWriter bw = null;
        ArrayList<String> lines = new ArrayList<>();
        int cleared = 0;
        try {
            br = new BufferedReader(new FileReader(source));
            String line;
            while ((line = br.readLine()) != null) {
                String[] args = line.split(":");
                if (args.length >= 4) {
                    if (Long.parseLong(args[3]) >= until) {
                        lines.add(line);
                        continue;
                    }
                }
                cleared++;
            }
            bw = new BufferedWriter(new FileWriter(source));
            for (String l : lines) {
                bw.write(l + "\n");
            }
        } catch (FileNotFoundException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return cleared;
        } catch (IOException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return cleared;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException ignored) {
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
     * @return List<String> * @see fr.xephi.authme.datasource.DataSource#autoPurgeDatabase(long)
     */
    @Override
    public List<String> autoPurgeDatabase(long until) {
        BufferedReader br = null;
        BufferedWriter bw = null;
        ArrayList<String> lines = new ArrayList<>();
        List<String> cleared = new ArrayList<>();
        try {
            br = new BufferedReader(new FileReader(source));
            String line;
            while ((line = br.readLine()) != null) {
                String[] args = line.split(":");
                if (args.length >= 4) {
                    if (Long.parseLong(args[3]) >= until) {
                        lines.add(line);
                        continue;
                    }
                }
                cleared.add(args[0]);
            }
            bw = new BufferedWriter(new FileWriter(source));
            for (String l : lines) {
                bw.write(l + "\n");
            }
        } catch (FileNotFoundException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return cleared;
        } catch (IOException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return cleared;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException ignored) {
                }
            }
        }
        return cleared;
    }

    /**
     * Method removeAuth.
     *
     * @param user String
     *
     * @return boolean * @see fr.xephi.authme.datasource.DataSource#removeAuth(String)
     */
    @Override
    public synchronized boolean removeAuth(String user) {
        if (!isAuthAvailable(user)) {
            return false;
        }
        BufferedReader br = null;
        BufferedWriter bw = null;
        ArrayList<String> lines = new ArrayList<>();
        try {
            br = new BufferedReader(new FileReader(source));
            String line;
            while ((line = br.readLine()) != null) {
                String[] args = line.split(":");
                if (args.length > 1 && !args[0].equals(user)) {
                    lines.add(line);
                }
            }
            bw = new BufferedWriter(new FileWriter(source));
            for (String l : lines) {
                bw.write(l + "\n");
            }
        } catch (FileNotFoundException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } catch (IOException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException ignored) {
                }
            }
        }
        return true;
    }

    /**
     * Method getAuth.
     *
     * @param user String
     *
     * @return PlayerAuth * @see fr.xephi.authme.datasource.DataSource#getAuth(String)
     */
    @Override
    public synchronized PlayerAuth getAuth(String user) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(source));
            String line;
            while ((line = br.readLine()) != null) {
                String[] args = line.split(":");
                if (args[0].equalsIgnoreCase(user)) {
                    switch (args.length) {
                        case 2:
                            return new PlayerAuth(args[0], args[1], "192.168.0.1", 0, "your@email.com", args[0]);
                        case 3:
                            return new PlayerAuth(args[0], args[1], args[2], 0, "your@email.com", args[0]);
                        case 4:
                            return new PlayerAuth(args[0], args[1], args[2], Long.parseLong(args[3]), "your@email.com", args[0]);
                        case 7:
                            return new PlayerAuth(args[0], args[1], args[2], Long.parseLong(args[3]), Double.parseDouble(args[4]), Double.parseDouble(args[5]), Double.parseDouble(args[6]), "unavailableworld", "your@email.com", args[0]);
                        case 8:
                            return new PlayerAuth(args[0], args[1], args[2], Long.parseLong(args[3]), Double.parseDouble(args[4]), Double.parseDouble(args[5]), Double.parseDouble(args[6]), args[7], "your@email.com", args[0]);
                        case 9:
                            return new PlayerAuth(args[0], args[1], args[2], Long.parseLong(args[3]), Double.parseDouble(args[4]), Double.parseDouble(args[5]), Double.parseDouble(args[6]), args[7], args[8], args[0]);
                    }
                }
            }
        } catch (FileNotFoundException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return null;
        } catch (IOException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return null;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
        }
        return null;
    }

    /**
     * Method close.
     *
     * @see fr.xephi.authme.datasource.DataSource#close()
     */
    @Override
    public synchronized void close() {
    }

    /**
     * Method reload.
     *
     * @see fr.xephi.authme.datasource.DataSource#reload()
     */
    @Override
    public void reload() {
    }

    /**
     * Method updateEmail.
     *
     * @param auth PlayerAuth
     *
     * @return boolean * @see fr.xephi.authme.datasource.DataSource#updateEmail(PlayerAuth)
     */
    @Override
    public boolean updateEmail(PlayerAuth auth) {
        if (!isAuthAvailable(auth.getNickname())) {
            return false;
        }
        PlayerAuth newAuth = null;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(source));
            String line;
            while ((line = br.readLine()) != null) {
                String[] args = line.split(":");
                if (args[0].equals(auth.getNickname())) {
                    newAuth = new PlayerAuth(args[0], args[1], args[2], Long.parseLong(args[3]), Double.parseDouble(args[4]), Double.parseDouble(args[5]), Double.parseDouble(args[6]), args[7], auth.getEmail(), args[0]);
                    break;
                }
            }
        } catch (FileNotFoundException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } catch (IOException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return false;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
        }
        if (newAuth != null) {
            removeAuth(auth.getNickname());
            saveAuth(newAuth);
        }
        return true;
    }

    /**
     * Method updateSalt.
     *
     * @param auth PlayerAuth
     *
     * @return boolean * @see fr.xephi.authme.datasource.DataSource#updateSalt(PlayerAuth)
     */
    @Override
    public boolean updateSalt(PlayerAuth auth) {
        return false;
    }

    /**
     * Method getAllAuthsByName.
     *
     * @param auth PlayerAuth
     *
     * @return List<String> * @see fr.xephi.authme.datasource.DataSource#getAllAuthsByName(PlayerAuth)
     */
    @Override
    public List<String> getAllAuthsByName(PlayerAuth auth) {
        BufferedReader br = null;
        List<String> countIp = new ArrayList<>();
        try {
            br = new BufferedReader(new FileReader(source));
            String line;
            while ((line = br.readLine()) != null) {
                String[] args = line.split(":");
                if (args.length > 3 && args[2].equals(auth.getIp())) {
                    countIp.add(args[0]);
                }
            }
            return countIp;
        } catch (FileNotFoundException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return new ArrayList<>();
        } catch (IOException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return new ArrayList<>();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Method getAllAuthsByIp.
     *
     * @param ip String
     *
     * @return List<String> * @see fr.xephi.authme.datasource.DataSource#getAllAuthsByIp(String)
     */
    @Override
    public List<String> getAllAuthsByIp(String ip) {
        BufferedReader br = null;
        List<String> countIp = new ArrayList<>();
        try {
            br = new BufferedReader(new FileReader(source));
            String line;
            while ((line = br.readLine()) != null) {
                String[] args = line.split(":");
                if (args.length > 3 && args[2].equals(ip)) {
                    countIp.add(args[0]);
                }
            }
            return countIp;
        } catch (FileNotFoundException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return new ArrayList<>();
        } catch (IOException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return new ArrayList<>();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Method getAllAuthsByEmail.
     *
     * @param email String
     *
     * @return List<String> * @see fr.xephi.authme.datasource.DataSource#getAllAuthsByEmail(String)
     */
    @Override
    public List<String> getAllAuthsByEmail(String email) {
        BufferedReader br = null;
        List<String> countEmail = new ArrayList<>();
        try {
            br = new BufferedReader(new FileReader(source));
            String line;
            while ((line = br.readLine()) != null) {
                String[] args = line.split(":");
                if (args.length > 8 && args[8].equals(email)) {
                    countEmail.add(args[0]);
                }
            }
            return countEmail;
        } catch (FileNotFoundException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return new ArrayList<>();
        } catch (IOException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return new ArrayList<>();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Method purgeBanned.
     *
     * @param banned List<String>
     *
     * @see fr.xephi.authme.datasource.DataSource#purgeBanned(List)
     */
    @Override
    public void purgeBanned(List<String> banned) {
        BufferedReader br = null;
        BufferedWriter bw = null;
        ArrayList<String> lines = new ArrayList<>();
        try {
            br = new BufferedReader(new FileReader(source));
            String line;
            while ((line = br.readLine()) != null) {
                String[] args = line.split(":");
                try {
                    if (banned.contains(args[0])) {
                        lines.add(line);
                    }
                } catch (NullPointerException | ArrayIndexOutOfBoundsException ignored) {
                }
            }
            bw = new BufferedWriter(new FileWriter(source));
            for (String l : lines) {
                bw.write(l + "\n");
            }

        } catch (IOException ex) {
            ConsoleLogger.showError(ex.getMessage());

        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Method getType.
     *
     * @return DataSourceType * @see fr.xephi.authme.datasource.DataSource#getType()
     */
    @Override
    public DataSourceType getType() {
        return DataSourceType.FILE;
    }

    /**
     * Method isLogged.
     *
     * @param user String
     *
     * @return boolean * @see fr.xephi.authme.datasource.DataSource#isLogged(String)
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
    public void setLogged(String user) {
    }

    /**
     * Method setUnlogged.
     *
     * @param user String
     *
     * @see fr.xephi.authme.datasource.DataSource#setUnlogged(String)
     */
    @Override
    public void setUnlogged(String user) {
    }

    /**
     * Method purgeLogged.
     *
     * @see fr.xephi.authme.datasource.DataSource#purgeLogged()
     */
    @Override
    public void purgeLogged() {
    }

    /**
     * Method getAccountsRegistered.
     *
     * @return int * @see fr.xephi.authme.datasource.DataSource#getAccountsRegistered()
     */
    @Override
    public int getAccountsRegistered() {
        BufferedReader br = null;
        int result = 0;
        try {
            br = new BufferedReader(new FileReader(source));
            while ((br.readLine()) != null) {
                result++;
            }
        } catch (Exception ex) {
            ConsoleLogger.showError(ex.getMessage());
            return result;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
        }
        return result;
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
    public void updateName(String oldOne, String newOne) {
        PlayerAuth auth = this.getAuth(oldOne);
        auth.setNickname(newOne);
        this.saveAuth(auth);
        this.removeAuth(oldOne);
    }

    /**
     * Method getAllAuths.
     *
     * @return List<PlayerAuth> * @see fr.xephi.authme.datasource.DataSource#getAllAuths()
     */
    @Override
    public List<PlayerAuth> getAllAuths() {
        BufferedReader br = null;
        List<PlayerAuth> auths = new ArrayList<>();
        try {
            br = new BufferedReader(new FileReader(source));
            String line;
            while ((line = br.readLine()) != null) {
                String[] args = line.split(":");
                switch (args.length) {
                    case 2:
                        auths.add(new PlayerAuth(args[0], args[1], "192.168.0.1", 0, "your@email.com", args[0]));
                        break;
                    case 3:
                        auths.add(new PlayerAuth(args[0], args[1], args[2], 0, "your@email.com", args[0]));
                        break;
                    case 4:
                        auths.add(new PlayerAuth(args[0], args[1], args[2], Long.parseLong(args[3]), "your@email.com", args[0]));
                        break;
                    case 7:
                        auths.add(new PlayerAuth(args[0], args[1], args[2], Long.parseLong(args[3]), Double.parseDouble(args[4]), Double.parseDouble(args[5]), Double.parseDouble(args[6]), "unavailableworld", "your@email.com", args[0]));
                        break;
                    case 8:
                        auths.add(new PlayerAuth(args[0], args[1], args[2], Long.parseLong(args[3]), Double.parseDouble(args[4]), Double.parseDouble(args[5]), Double.parseDouble(args[6]), args[7], "your@email.com", args[0]));
                        break;
                    case 9:
                        auths.add(new PlayerAuth(args[0], args[1], args[2], Long.parseLong(args[3]), Double.parseDouble(args[4]), Double.parseDouble(args[5]), Double.parseDouble(args[6]), args[7], args[8], args[0]));
                        break;
                }
            }
        } catch (FileNotFoundException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return auths;
        } catch (IOException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return auths;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
        }
        return auths;
    }

    /**
     * Method getLoggedPlayers.
     *
     * @return List<PlayerAuth> * @see fr.xephi.authme.datasource.DataSource#getLoggedPlayers()
     */
    @Override
    public List<PlayerAuth> getLoggedPlayers() {
        return new ArrayList<>();
    }

	@Override
	public Connection getConnection() throws SQLException {
		return null;
	}
}
