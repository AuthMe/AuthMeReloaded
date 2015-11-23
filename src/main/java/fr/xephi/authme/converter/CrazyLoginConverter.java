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
 * @version $Revision: 1.0 $
 */
public class CrazyLoginConverter implements Converter {

    public AuthMe instance;
    public DataSource database;
    public CommandSender sender;

    /**
     * Constructor for CrazyLoginConverter.
     *
     * @param instance AuthMe
     * @param sender   CommandSender
     */
    public CrazyLoginConverter(AuthMe instance, CommandSender sender) {
        this.instance = instance;
        this.database = instance.database;
        this.sender = sender;
    }

    /**
     * Method getInstance.
     *
     * @return CrazyLoginConverter
     */
    public CrazyLoginConverter getInstance() {
        return this;
    }

    /**
     * Method run.
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        String fileName = Settings.crazyloginFileName;
        try {
            File source = new File(AuthMe.getInstance().getDataFolder() + File.separator + fileName);
            if (!source.exists()) {
                sender.sendMessage("Error while trying to import datas, please put " + fileName + " in AuthMe folder!");
                return;
            }
            String line;
            BufferedReader users = new BufferedReader(new FileReader(source));
            while ((line = users.readLine()) != null) {
                if (line.contains("|")) {
                    String[] args = line.split("\\|");
                    if (args.length < 2)
                        continue;
                    if (args[0].equalsIgnoreCase("name"))
                        continue;
                    String playerName = args[0].toLowerCase();
                    String psw = args[1];
                    if (psw != null) {
                        PlayerAuth auth = new PlayerAuth(playerName, psw, "127.0.0.1", System.currentTimeMillis(), playerName);
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
