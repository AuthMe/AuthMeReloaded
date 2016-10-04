package fr.xephi.authme.datasource;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.security.crypts.HashedPassword;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Deprecated flat file datasource. The only method guaranteed to work is {@link FlatFile#getAllAuths()}
 * as to migrate the entries to {@link SQLite} when AuthMe starts.
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

    public FlatFile(File source) throws IOException {
        this.source = source;
        if (!source.exists() && !source.createNewFile()) {
            throw new IOException("Could not create file '" + source.getPath() + "'");
        }
    }

    @Override
    public void reload() {
        throw new UnsupportedOperationException("Flatfile no longer supported");
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
        } catch (IOException ex) {
            ConsoleLogger.warning(ex.getMessage());
            return false;
        } finally {
            silentClose(br);
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
            bw.write(auth.getNickname() + ":" + auth.getPassword().getHash() + ":" + auth.getIp() + ":" + auth.getLastLogin() + ":" + auth.getQuitLocX() + ":" + auth.getQuitLocY() + ":" + auth.getQuitLocZ() + ":" + auth.getWorld() + ":" + auth.getEmail() + "\n");
        } catch (IOException ex) {
            ConsoleLogger.warning(ex.getMessage());
            return false;
        } finally {
            silentClose(bw);
        }
        return true;
    }

    @Override
    public synchronized boolean updatePassword(PlayerAuth auth) {
        return updatePassword(auth.getNickname(), auth.getPassword());
    }

    @Override
    // Note ljacqu 20151230: This does not persist the salt; it is not supported in flat file.
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
                    newAuth = buildAuthFromArray(args);
                    if (newAuth != null) {
                        newAuth.setPassword(password);
                    }
                    break;
                }
            }
        } catch (IOException ex) {
            ConsoleLogger.warning(ex.getMessage());
            return false;
        } finally {
            silentClose(br);
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
                    newAuth = buildAuthFromArray(args);
                    if (newAuth != null) {
                        newAuth.setLastLogin(auth.getLastLogin());
                        newAuth.setIp(auth.getIp());
                    }
                    break;
                }
            }
        } catch (IOException ex) {
            ConsoleLogger.warning(ex.getMessage());
            return false;
        } finally {
            silentClose(br);
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
                    newAuth = buildAuthFromArray(args);
                    if (newAuth != null) {
                        newAuth.setQuitLocX(auth.getQuitLocX());
                        newAuth.setQuitLocY(auth.getQuitLocY());
                        newAuth.setQuitLocZ(auth.getQuitLocZ());
                        newAuth.setWorld(auth.getWorld());
                        newAuth.setEmail(auth.getEmail());
                    }
                    break;
                }
            }
        } catch (IOException ex) {
            ConsoleLogger.warning(ex.getMessage());
            return false;
        } finally {
            silentClose(br);
        }
        if (newAuth != null) {
            removeAuth(auth.getNickname());
            saveAuth(newAuth);
        }
        return true;
    }

    @Override
    public Set<String> getRecordsToPurge(long until, boolean includeEntriesWithLastLoginZero) {
        throw new UnsupportedOperationException("Flat file no longer supported");
    }

    @Override
    public void purgeRecords(Collection<String> toPurge) {
        throw new UnsupportedOperationException("Flat file no longer supported");
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
        } catch (IOException ex) {
            ConsoleLogger.warning(ex.getMessage());
            return false;
        } finally {
            silentClose(br);
            silentClose(bw);
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
                    return buildAuthFromArray(args);
                }
            }
        } catch (IOException ex) {
            ConsoleLogger.warning(ex.getMessage());
            return null;
        } finally {
            silentClose(br);
        }
        return null;
    }

    @Override
    public synchronized void close() {
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
                    newAuth = buildAuthFromArray(args);
                    if (newAuth != null) {
                        newAuth.setEmail(auth.getEmail());
                    }
                    break;
                }
            }
        } catch (FileNotFoundException ex) {
            ConsoleLogger.warning(ex.getMessage());
            return false;
        } catch (IOException ex) {
            ConsoleLogger.warning(ex.getMessage());
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
            ConsoleLogger.warning(ex.getMessage());
            return new ArrayList<>();
        } catch (IOException ex) {
            ConsoleLogger.warning(ex.getMessage());
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
            ConsoleLogger.warning(ex.getMessage());
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
            ConsoleLogger.warning(ex.getMessage());
            return result;
        } finally {
            silentClose(br);
        }
        return result;
    }

    @Override
    public boolean updateRealName(String user, String realName) {
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
                PlayerAuth auth = buildAuthFromArray(args);
                if (auth != null) {
                    auths.add(auth);
                }
            }
        } catch (IOException ex) {
            ConsoleLogger.logException("Error while getting auths from flatfile:", ex);
        } finally {
            silentClose(br);
        }
        return auths;
    }

    @Override
    public List<PlayerAuth> getLoggedPlayers() {
        throw new UnsupportedOperationException("Flat file no longer supported");
    }

    private static PlayerAuth buildAuthFromArray(String[] args) {
        // Format allows 2, 3, 4, 7, 8, 9 fields. Anything else is unknown
        if (args.length >= 2 && args.length <= 9 && args.length != 5 && args.length != 6) {
            PlayerAuth.Builder builder = PlayerAuth.builder()
                .name(args[0]).realName(args[0]).password(args[1], null);

            if (args.length >= 3)   builder.ip(args[2]);
            if (args.length >= 4)   builder.lastLogin(Long.parseLong(args[3]));
            if (args.length >= 7) {
                builder.locX(Double.parseDouble(args[4]))
                    .locY(Double.parseDouble(args[5]))
                    .locZ(Double.parseDouble(args[6]));
            }
            if (args.length >= 8)   builder.locWorld(args[7]);
            if (args.length >= 9)   builder.email(args[8]);
            return builder.build();
        }
        return null;
    }

    private static void silentClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
                // silent close
            }
        }
    }
}
