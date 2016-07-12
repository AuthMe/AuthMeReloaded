package fr.xephi.authme.converter;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.FlatFile;
import fr.xephi.authme.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Mandatory migration from the deprecated flat file datasource to SQLite.
 */
public class ForceFlatToSqlite {

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
    public void run() {
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
            ConsoleLogger.warning("Warning: skipped conversion for players which were already in SQLite: "
                + StringUtils.join(", ", skippedPlayers));
        }
        ConsoleLogger.info("Database successfully converted from " + source.getClass().getSimpleName()
            + " to " + destination.getClass().getSimpleName());
    }
}
