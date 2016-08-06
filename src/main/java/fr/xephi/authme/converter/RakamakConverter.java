package fr.xephi.authme.converter;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.ConverterSettings;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
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

    private final DataSource database;
    private final Settings settings;
    private final File pluginFolder;
    private final PasswordSecurity passwordSecurity;

    @Inject
    RakamakConverter(@DataFolder File dataFolder, DataSource dataSource, Settings settings,
                     PasswordSecurity passwordSecurity) {
        this.database = dataSource;
        this.settings = settings;
        this.pluginFolder = dataFolder;
        this.passwordSecurity = passwordSecurity;
    }

    @Override
    // TODO ljacqu 20151229: Restructure this into smaller portions
    public void execute(CommandSender sender) {
        boolean useIP = settings.getProperty(ConverterSettings.RAKAMAK_USE_IP);
        String fileName = settings.getProperty(ConverterSettings.RAKAMAK_FILE_NAME);
        String ipFileName = settings.getProperty(ConverterSettings.RAKAMAK_IP_FILE_NAME);
        File source = new File(pluginFolder, fileName);
        File ipfiles = new File(pluginFolder, ipFileName);
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
            while ((line = users.readLine()) != null) {
                if (line.contains("=")) {
                    String[] arguments = line.split("=");
                    HashedPassword hashedPassword = passwordSecurity.computeHash(arguments[1], arguments[0]);
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
                    .lastLogin(0)
                    .build();
                database.saveAuth(auth);
            }
            ConsoleLogger.info("Rakamak database has been imported correctly");
            sender.sendMessage("Rakamak database has been imported correctly");
        } catch (IOException ex) {
            ConsoleLogger.logException("Can't open the rakamak database file! Does it exist?", ex);
        }
    }
}
