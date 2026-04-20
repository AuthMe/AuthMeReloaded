package fr.xephi.authme.datasource.converter;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.ConverterSettings;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

/**
 * Converter for CrazyLogin to AuthMe.
 */
public class CrazyLoginConverter implements Converter {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(CrazyLoginConverter.class);

    private final DataSource database;
    private final Settings settings;
    private final File dataFolder;

    @Inject
    CrazyLoginConverter(@DataFolder File dataFolder, DataSource dataSource, Settings settings) {
        this.dataFolder = dataFolder;
        this.database = dataSource;
        this.settings = settings;
    }

    @Override
    public void execute(CommandSender sender) {
        String fileName = settings.getProperty(ConverterSettings.CRAZYLOGIN_FILE_NAME);
        File source = new File(dataFolder, fileName);
        if (!source.exists()) {
            sender.sendMessage("CrazyLogin file not found, please put " + fileName + " in AuthMe folder!");
            return;
        }

        String line;
        try (BufferedReader users = new BufferedReader(new FileReader(source))) {
            while ((line = users.readLine()) != null) {
                if (line.contains("|")) {
                    migrateAccount(line);
                }
            }
            logger.info("CrazyLogin database has been imported correctly");
        } catch (IOException ex) {
            logger.warning("Can't open the crazylogin database file! Does it exist?");
            logger.logException("Encountered", ex);
        }
    }

    /**
     * Moves an account from CrazyLogin to the AuthMe database.
     *
     * @param line line read from the CrazyLogin file (one account)
     */
    private void migrateAccount(String line) {
        String[] args = line.split("\\|");
        if (args.length < 2 || "name".equalsIgnoreCase(args[0])) {
            return;
        }
        String playerName = args[0];
        String password = args[1];
        if (password != null) {
            PlayerAuth auth = PlayerAuth.builder()
                .name(playerName.toLowerCase(Locale.ROOT))
                .realName(playerName)
                .password(password, null)
                .build();
            database.saveAuth(auth);
        }
    }

}
