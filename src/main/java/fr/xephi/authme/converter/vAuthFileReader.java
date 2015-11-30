package fr.xephi.authme.converter;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.Scanner;
import java.util.UUID;

/**
 */
public class vAuthFileReader {

    public final AuthMe plugin;
    public final DataSource database;
    public final CommandSender sender;

    /**
     * Constructor for vAuthFileReader.
     *
     * @param plugin AuthMe
     * @param sender CommandSender
     */
    public vAuthFileReader(AuthMe plugin, CommandSender sender) {
        this.plugin = plugin;
        this.database = plugin.database;
        this.sender = sender;
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
                        playerName = Bukkit.getOfflinePlayer(UUID.fromString(name)).getName();
                    } catch (Exception | NoSuchMethodError e) {
                        playerName = getName(UUID.fromString(name));
                    }
                    if (playerName == null)
                        continue;
                    auth = new PlayerAuth(playerName.toLowerCase(), password, "127.0.0.1", System.currentTimeMillis(), "your@email.com", playerName);
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

    /**
     * Method getName.
     *
     * @param uuid UUID
     *
     * @return String
     */
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
