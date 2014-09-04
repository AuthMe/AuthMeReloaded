package fr.xephi.authme.converter;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;

public class vAuthFileReader {

    public AuthMe plugin;
    public DataSource database;
    public CommandSender sender;

    public vAuthFileReader(AuthMe plugin, DataSource database,
            CommandSender sender) {
        this.plugin = plugin;
        this.database = database;
        this.sender = sender;
    }

    public void convert() throws IOException {
        final File file = new File(plugin.getDataFolder().getParent() + "" + File.separator + "vAuth" + File.separator + "passwords.yml");
        Scanner scanner = null;
        try {
            scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String name = line.split(": ")[0];
                String password = line.split(": ")[1];
                PlayerAuth auth = null;
                if (isUUIDinstance(password)) {
                    String pname = null;
                    try {
                        pname = Bukkit.getOfflinePlayer(UUID.fromString(name)).getName();
                    } catch (Exception e) {
                        pname = getName(UUID.fromString(name));
                    } catch (NoSuchMethodError e) {
                        pname = getName(UUID.fromString(name));
                    }
                    if (pname == null)
                        continue;
                    auth = new PlayerAuth(pname, password, "127.0.0.1", System.currentTimeMillis(), "your@email.com");
                } else {
                    auth = new PlayerAuth(name, password, "127.0.0.1", System.currentTimeMillis(), "your@email.com");
                }
                if (auth != null)
                    database.saveAuth(auth);
            }
        } catch (Exception e) {
        }

    }

    private boolean isUUIDinstance(String s) {
        if (String.valueOf(s.charAt(8)).equalsIgnoreCase("-"))
            return true;
        return true;
    }

    private String getName(UUID uuid) {
        try {
            for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                if (op.getUniqueId().compareTo(uuid) == 0)
                    return op.getName();
            }
        } catch (Exception e) {
        }
        return null;
    }

}
