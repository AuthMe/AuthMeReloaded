package fr.xephi.authme.converter;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.settings.Settings;
import org.bukkit.command.CommandSender;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author Xephi59
 */
public class CrazyLoginConverter implements Converter {

    private final DataSource database;
    private final CommandSender sender;

    /**
     * Constructor for CrazyLoginConverter.
     *
     * @param instance AuthMe
     * @param sender   CommandSender
     */
    public CrazyLoginConverter(AuthMe instance, CommandSender sender) {
        this.database = instance.getDataSource();
        this.sender = sender;
    }

    @Override
    public void run() {
        String fileName = Settings.crazyloginFileName;
        try {
            File source = new File(AuthMe.getInstance().getDataFolder() + File.separator + fileName);
            if (!source.exists()) {
                sender.sendMessage("Error while trying to import data, please put " + fileName + " in AuthMe folder!");
                return;
            }
            String line;
            BufferedReader users = new BufferedReader(new FileReader(source));
            while ((line = users.readLine()) != null) {
                if (line.contains("|")) {
                    String[] args = line.split("\\|");
                    if (args.length < 2 || "name".equalsIgnoreCase(args[0])) {
                        continue;
                    }
                    String playerName = args[0];
                    String psw = args[1];
                    if (psw != null) {
                        PlayerAuth auth = PlayerAuth.builder()
                            .name(playerName.toLowerCase())
                            .realName(playerName)
                            .password(psw, null)
                            .build();
                        database.saveAuth(auth);
                    }
                }
            }
            users.close();
            ConsoleLogger.info("CrazyLogin database has been imported correctly");
        } catch (IOException ex) {
            ConsoleLogger.showError(ex.getMessage());
            ConsoleLogger.showError("Can't open the crazylogin database file! Does it exist?");
        }
    }

}
