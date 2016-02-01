package fr.xephi.authme.converter;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.Settings;
import org.bukkit.command.CommandSender;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * @author Xephi59
 */
public class RakamakConverter implements Converter {

    public final AuthMe instance;
    public final DataSource database;
    public final CommandSender sender;

    public RakamakConverter(AuthMe instance, CommandSender sender) {
        this.instance = instance;
        this.database = instance.getDataSource();
        this.sender = sender;
    }

    @Override
    // TODO ljacqu 20151229: Restructure this into smaller portions
    public void run() {
        HashAlgorithm hash = Settings.getPasswordHash;
        boolean useIP = Settings.rakamakUseIp;
        String fileName = Settings.rakamakUsers;
        String ipFileName = Settings.rakamakUsersIp;
        File source = new File(Settings.PLUGIN_FOLDER, fileName);
        File ipfiles = new File(Settings.PLUGIN_FOLDER, ipFileName);
        HashMap<String, String> playerIP = new HashMap<>();
        HashMap<String, HashedPassword> playerPSW = new HashMap<>();
        try {
            BufferedReader users;
            BufferedReader ipFile;
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
            PasswordSecurity passwordSecurity = instance.getPasswordSecurity();
            while ((line = users.readLine()) != null) {
                if (line.contains("=")) {
                    String[] arguments = line.split("=");
                    HashedPassword hashedPassword = passwordSecurity.computeHash(hash, arguments[1], arguments[0]);
                    playerPSW.put(arguments[0], hashedPassword);

                }
            }
            users.close();
            for (Entry<String, HashedPassword> m : playerPSW.entrySet()) {
                String playerName = m.getKey();
                HashedPassword psw = playerPSW.get(playerName);
                String ip = useIP ? playerIP.get(playerName) : "127.0.0.1";
                PlayerAuth auth = PlayerAuth.builder()
                    .name(playerName)
                    .realName(playerName)
                    .ip(ip)
                    .password(psw)
                    .lastLogin(System.currentTimeMillis())
                    .build();
                database.saveAuth(auth);
            }
            ConsoleLogger.info("Rakamak database has been imported correctly");
            sender.sendMessage("Rakamak database has been imported correctly");
        } catch (IOException ex) {
            ConsoleLogger.showError(ex.getMessage());
            sender.sendMessage("Can't open the rakamak database file! Does it exist?");
        }
    }
}
