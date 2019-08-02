package fr.xephi.authme.datasource.converter;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.ConverterSettings;
import fr.xephi.authme.util.Utils;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Xephi59
 */
public class RakamakConverter implements Converter {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(RakamakConverter.class);
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
    //TODO ljacqu 20151229: Restructure this into smaller portions
    public void execute(CommandSender sender) {
        boolean useIp = settings.getProperty(ConverterSettings.RAKAMAK_USE_IP);
        String fileName = settings.getProperty(ConverterSettings.RAKAMAK_FILE_NAME);
        String ipFileName = settings.getProperty(ConverterSettings.RAKAMAK_IP_FILE_NAME);
        File source = new File(pluginFolder, fileName);
        File ipFiles = new File(pluginFolder, ipFileName);
        Map<String, String> playerIp = new HashMap<>();
        Map<String, HashedPassword> playerPassword = new HashMap<>();
        try {
            BufferedReader ipFile = new BufferedReader(new FileReader(ipFiles));
            String line;
            if (useIp) {
                String tempLine;
                while ((tempLine = ipFile.readLine()) != null) {
                    if (tempLine.contains("=")) {
                        String[] args = tempLine.split("=");
                        playerIp.put(args[0], args[1]);
                    }
                }
            }
            ipFile.close();

            BufferedReader users = new BufferedReader(new FileReader(source));
            while ((line = users.readLine()) != null) {
                if (line.contains("=")) {
                    String[] arguments = line.split("=");
                    HashedPassword hashedPassword = passwordSecurity.computeHash(arguments[1], arguments[0]);
                    playerPassword.put(arguments[0], hashedPassword);

                }
            }
            users.close();
            for (Entry<String, HashedPassword> m : playerPassword.entrySet()) {
                String playerName = m.getKey();
                HashedPassword psw = playerPassword.get(playerName);
                String ip = playerIp.get(playerName);
                PlayerAuth auth = PlayerAuth.builder()
                    .name(playerName)
                    .realName(playerName)
                    .lastIp(ip)
                    .password(psw)
                    .build();
                database.saveAuth(auth);
                database.updateSession(auth);
            }
            Utils.logAndSendMessage(sender, "Rakamak database has been imported successfully");
        } catch (IOException ex) {
            logger.logException("Can't open the rakamak database file! Does it exist?", ex);
        }
    }
}
