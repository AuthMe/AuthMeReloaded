package fr.xephi.authme.converter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.command.CommandSender;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.settings.Settings;

/**
 *
 * @author Xephi59
 */
public class RakamakConverter implements Converter {

    public AuthMe instance;
    public DataSource database;
    public CommandSender sender;

    public RakamakConverter(AuthMe instance, DataSource database,
            CommandSender sender) {
        this.instance = instance;
        this.database = database;
        this.sender = sender;
    }

    public RakamakConverter getInstance() {
        return this;
    }

    private static Boolean useIP;
    private static String fileName;
    private static String ipFileName;
    private static File source;
    private static File ipfiles;

    @Override
    public void run() {
        HashAlgorithm hash = Settings.getPasswordHash;
        useIP = Settings.rakamakUseIp;
        fileName = Settings.rakamakUsers;
        ipFileName = Settings.rakamakUsersIp;
        HashMap<String, String> playerIP = new HashMap<String, String>();
        HashMap<String, String> playerPSW = new HashMap<String, String>();
        try {
            source = new File(AuthMe.getInstance().getDataFolder() + File.separator + fileName);
            ipfiles = new File(AuthMe.getInstance().getDataFolder() + File.separator + ipFileName);
            source.createNewFile();
            ipfiles.createNewFile();
            BufferedReader users = null;
            BufferedReader ipFile = null;
            ipFile = new BufferedReader(new FileReader(ipfiles));
            String line;
            if (useIP) {
                String tempLine;
                while ((tempLine = ipFile.readLine()) != null) {
                    if (tempLine.contains("=")) {
                        String[] args = tempLine.split("=");
                        playerIP.put(args[0], args[1]);
                    }
                }
            }
            ipFile.close();
            users = new BufferedReader(new FileReader(source));
            while ((line = users.readLine()) != null) {
                if (line.contains("=")) {
                    String[] arguments = line.split("=");
                    try {
                        playerPSW.put(arguments[0], PasswordSecurity.getHash(hash, arguments[1], arguments[0]));
                    } catch (NoSuchAlgorithmException e) {
                        ConsoleLogger.showError(e.getMessage());
                    }
                }
            }
            users.close();
            for (Entry<String, String> m : playerPSW.entrySet()) {
                String player = m.getKey();
                String psw = playerPSW.get(player);
                String ip;
                if (useIP) {
                    ip = playerIP.get(player);
                } else {
                    ip = "127.0.0.1";
                }
                PlayerAuth auth = new PlayerAuth(player, psw, ip, System.currentTimeMillis());
                if (PasswordSecurity.userSalt.containsKey(player))
                    auth.setSalt(PasswordSecurity.userSalt.get(player));
                database.saveAuth(auth);
            }
            ConsoleLogger.info("Rakamak database has been imported correctly");
            sender.sendMessage("Rakamak database has been imported correctly");
        } catch (FileNotFoundException ex) {
            ConsoleLogger.showError(ex.getMessage());
            sender.sendMessage("Error file not found");
        } catch (IOException ex) {
            ConsoleLogger.showError(ex.getMessage());
            sender.sendMessage("Error IOException");
        }
    }
}
