package fr.xephi.authme.converter;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.FlatFile;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * Mandatory migration from the deprecated flat file datasource to SQLite.
 */
public class ForceFlatToSqlite implements Converter {

    private final DataSource source;
    private final DataSource destination;

    /**
     * Constructor.
     *
     * @param source The datasource to convert (flatfile)
     * @param destination The datasource to copy the data to (sqlite)
     */
    public ForceFlatToSqlite(FlatFile source, DataSource destination) {
        this.source = source;
        this.destination = destination;
    }

    /**
     * Perform the conversion.
     */
    @Override
    // Note ljacqu 20160527: CommandSender is null here; it is only present because of the interface it implements
    public void execute(CommandSender sender) {
        List<String> skippedPlayers = new ArrayList<>();
        for (PlayerAuth auth : source.getAllAuths()) {
            if (destination.isAuthAvailable(auth.getNickname())) {
                skippedPlayers.add(auth.getNickname());
            } else {
                destination.saveAuth(auth);
                destination.updateQuitLoc(auth);
            }
        }

        if (!skippedPlayers.isEmpty()) {
            ConsoleLogger.showError("Warning: skipped conversion for players which were already in SQLite: "
                + StringUtils.join(", ", skippedPlayers));
        }
        ConsoleLogger.info("Database successfully converted from " + source.getClass().getSimpleName()
            + " to " + destination.getClass().getSimpleName());
    }
}
