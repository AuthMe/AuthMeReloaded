package fr.xephi.authme.converter;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.UUID;

import static fr.xephi.authme.util.StringUtils.makePath;

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
        final File file = new File(plugin.getDataFolder().getParent(), makePath("vAuth", "passwords.yml"));
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
                    auth = PlayerAuth.builder()
                        .name(pname.toLowerCase())
                        .realName(pname)
                        .password(password, null).build();
                } else {
                    auth = PlayerAuth.builder()
                        .name(name.toLowerCase())
                        .realName(name)
                        .password(password, null).build();
                }
                database.saveAuth(auth);
            }
            scanner.close();
        } catch (IOException e) {
            ConsoleLogger.logException("Error while trying to import some vAuth data", e);
        }

    }

    private static boolean isUuidInstance(String s) {
        return s.length() > 8 && s.charAt(8) == '-';
    }

    private String getName(UUID uuid) {
        for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
            if (op.getUniqueId().compareTo(uuid) == 0) {
                return op.getName();
            }
        }
        return null;
    }

}
