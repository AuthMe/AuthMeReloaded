package fr.xephi.authme.datasource.converter;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Scanner;
import java.util.UUID;

import static fr.xephi.authme.util.FileUtils.makePath;

public class VAuthConverter implements Converter {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(VAuthConverter.class);
    private final DataSource dataSource;
    private final File vAuthPasswordsFile;

    @Inject
    VAuthConverter(@DataFolder File dataFolder, DataSource dataSource) {
        vAuthPasswordsFile = new File(dataFolder.getParent(), makePath("vAuth", "passwords.yml"));
        this.dataSource = dataSource;
    }

    @Override
    public void execute(CommandSender sender) {
        try (Scanner scanner = new Scanner(vAuthPasswordsFile)) {
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
                    if (pname == null) {
                        continue;
                    }
                    auth = PlayerAuth.builder()
                        .name(pname.toLowerCase(Locale.ROOT))
                        .realName(pname)
                        .password(password, null).build();
                } else {
                    auth = PlayerAuth.builder()
                        .name(name.toLowerCase(Locale.ROOT))
                        .realName(name)
                        .password(password, null).build();
                }
                dataSource.saveAuth(auth);
            }
        } catch (IOException e) {
            logger.logException("Error while trying to import some vAuth data", e);
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
