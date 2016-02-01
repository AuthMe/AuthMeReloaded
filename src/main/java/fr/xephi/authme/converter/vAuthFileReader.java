package fr.xephi.authme.converter;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.File;
import java.util.Scanner;
import java.util.UUID;

class vAuthFileReader {

    private final AuthMe plugin;
    private final DataSource database;

    /**
     * Constructor for vAuthFileReader.
     *
     * @param plugin AuthMe
     */
    public vAuthFileReader(AuthMe plugin) {
        this.plugin = plugin;
        this.database = plugin.getDataSource();
    }

    public void convert() {
        final File file = new File(plugin.getDataFolder().getParent() + "" + File.separator + "vAuth" + File.separator + "passwords.yml");
        Scanner scanner;
        try {
            scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String name = line.split(": ")[0];
                String password = line.split(": ")[1];
                PlayerAuth auth;
                if (isUuidInstance(password)) {
                    String pname;
                    try {
                        pname = Bukkit.getOfflinePlayer(UUID.fromString(name)).getName();
                    } catch (Exception | NoSuchMethodError e) {
                        pname = getName(UUID.fromString(name));
                    }
                    if (pname == null)
                        continue;
                    auth = new PlayerAuth(pname.toLowerCase(), password, "127.0.0.1", System.currentTimeMillis(), "your@email.com", pname);
                } else {
                    auth = new PlayerAuth(name.toLowerCase(), password, "127.0.0.1", System.currentTimeMillis(), "your@email.com", name);
                }
                database.saveAuth(auth);
            }
            scanner.close();
        } catch (Exception e) {
            ConsoleLogger.writeStackTrace(e);
        }

    }

    private static boolean isUuidInstance(String s) {
        return s.length() > 8 && s.charAt(8) == '-';
    }

    private String getName(UUID uuid) {
        try {
            for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                if (op.getUniqueId().compareTo(uuid) == 0)
                    return op.getName();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

}
