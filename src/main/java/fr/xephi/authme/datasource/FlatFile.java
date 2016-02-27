package fr.xephi.authme.datasource;

import com.google.common.annotations.VisibleForTesting;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.Settings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
        AuthMe instance = AuthMe.getInstance();

        source = new File(instance.getDataFolder(), "auths.db");
        try {
            source.createNewFile();
        } catch (IOException e) {
            ConsoleLogger.logException("Cannot open flatfile", e);
            if (Settings.isStopEnabled) {
                ConsoleLogger.showError("Can't use FLAT FILE... SHUTDOWN...");
                instance.getServer().shutdown();
            }
            if (!Settings.isStopEnabled) {
                instance.getServer().getPluginManager().disablePlugin(instance);
            }
        }
    }

    @VisibleForTesting
    public FlatFile(File source) {
        this.source = source;
    }

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

    @Override
    public HashedPassword getPassword(String user) {
        PlayerAuth auth = getAuth(user);
        if (auth != null) {
            return auth.getPassword();
        }
        return null;
    }

    @Override
    public synchronized boolean saveAuth(PlayerAuth auth) {
        if (isAuthAvailable(auth.getNickname())) {
            return false;
        }
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(source, true));
            bw.write(auth.getNickname() + ":" + auth.getPassword() + ":" + auth.getIp() + ":" + auth.getLastLogin() + ":" + auth.getQuitLocX() + ":" + auth.getQuitLocY() + ":" + auth.getQuitLocZ() + ":" + auth.getWorld() + ":" + auth.getEmail() + "\n");
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

    @Override
    public synchronized boolean updatePassword(PlayerAuth auth) {
        return updatePassword(auth.getNickname(), auth.getPassword());
    }

    @Override
    public boolean updatePassword(String user, HashedPassword password) {
        user = user.toLowerCase();
        if (!isAuthAvailable(user)) {
            return false;
        }
        PlayerAuth newAuth = null;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(source));
            String line;
            while ((line = br.readLine()) != null) {
                String[] args = line.split(":");
                if (args[0].equals(user)) {
                    // Note ljacqu 20151230: This does not persist the salt; it is not supported in flat file.
                    switch (args.length) {
                        case 4: {
                            newAuth = new PlayerAuth(args[0], password.getHash(), args[2], Long.parseLong(args[3]), 0, 0, 0, "world", "your@email.com", args[0]);
                            break;
                        }
                        case 7: {
                            newAuth = new PlayerAuth(args[0], password.getHash(), args[2], Long.parseLong(args[3]), Double.parseDouble(args[4]), Double.parseDouble(args[5]), Double.parseDouble(args[6]), "world", "your@email.com", args[0]);
                            break;
                        }
                        case 8: {
                            newAuth = new PlayerAuth(args[0], password.getHash(), args[2], Long.parseLong(args[3]), Double.parseDouble(args[4]), Double.parseDouble(args[5]), Double.parseDouble(args[6]), args[7], "your@email.com", args[0]);
                            break;
                        }
                        case 9: {
                            newAuth = new PlayerAuth(args[0], password.getHash(), args[2], Long.parseLong(args[3]), Double.parseDouble(args[4]), Double.parseDouble(args[5]), Double.parseDouble(args[6]), args[7], args[8], args[0]);
                            break;
                        }
                        default: {
                            newAuth = new PlayerAuth(args[0], password.getHash(), args[2], 0, 0, 0, 0, "world", "your@email.com", args[0]);
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
            removeAuth(user);
            saveAuth(newAuth);
        }
        return true;
    }

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

    @Override
    public synchronized void close() {
    }

    @Override
    public void reload() {
    }

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

    @Override
    public int countAuthsByEmail(String email) {
        BufferedReader br = null;
        int countEmail = 0;
        try {
            br = new BufferedReader(new FileReader(source));
            String line;
            while ((line = br.readLine()) != null) {
                String[] args = line.split(":");
                if (args.length > 8 && args[8].equals(email)) {
                    ++countEmail;
                }
            }
            return countEmail;
        } catch (IOException ex) {
            ConsoleLogger.showError(ex.getMessage());
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
        }
        return 0;
    }

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

    @Override
    public DataSourceType getType() {
        return DataSourceType.FILE;
    }

    @Override
    public boolean isLogged(String user) {
        return PlayerCache.getInstance().isAuthenticated(user);
    }

    @Override
    public void setLogged(String user) {
    }

    @Override
    public void setUnlogged(String user) {
    }

    @Override
    public void purgeLogged() {
    }

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

    @Override
    public boolean updateRealName(String user, String realName) {
        throw new UnsupportedOperationException("Flat file no longer supported");
    }

    @Override
    public boolean updateIp(String user, String ip) {
        throw new UnsupportedOperationException("Flat file no longer supported");
    }

    @Override
    public List<PlayerAuth> getAllAuths() {
        BufferedReader br = null;
        List<PlayerAuth> auths = new ArrayList<>();
        try {
            br = new BufferedReader(new FileReader(source));
            String line;
            while ((line = br.readLine()) != null) {
                String[] args = line.split(":");
                // We expect to encounter 2, 3, 4, 7, 8 or 9 fields. Ignore the line otherwise
                if (args.length >= 2 && args.length != 5 && args.length != 6 && args.length <= 9) {
                    PlayerAuth.Builder builder = PlayerAuth.builder()
                        .name(args[0]).realName(args[0])
                        .password(args[1], null);
                    if (args.length >= 3)   builder.ip(args[2]);
                    if (args.length >= 4)   builder.lastLogin(Long.parseLong(args[3]));
                    if (args.length >= 7) {
                        builder.locX(Double.parseDouble(args[4]))
                            .locY(Double.parseDouble(args[5]))
                            .locZ(Double.parseDouble(args[6]));
                    }
                    if (args.length >= 8)   builder.locWorld(args[7]);
                    if (args.length >= 9)   builder.email(args[8]);
                    auths.add(builder.build());
                }
            }
        } catch (IOException ex) {
            ConsoleLogger.logException("Error while getting auths from flatfile:", ex);
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

    @Override
    public List<PlayerAuth> getLoggedPlayers() {
        throw new UnsupportedOperationException("Flat file no longer supported");
    }

    @Override
    public boolean isEmailStored(String email) {
        throw new UnsupportedOperationException("Flat file no longer supported");
    }
}
