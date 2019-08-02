package fr.xephi.authme.datasource.converter;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.DataSourceType;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

import static fr.xephi.authme.util.Utils.logAndSendMessage;

/**
 * Converts from one AuthMe data source type to another.
 *
 * @param <S> the source type to convert from
 */
public abstract class AbstractDataSourceConverter<S extends DataSource> implements Converter {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(MySqlToSqlite.class);

    private final DataSource destination;
    private final DataSourceType destinationType;

    /**
     * Constructor.
     *
     * @param destination the data source to convert to
     * @param destinationType the data source type of the destination. The given data source is checked that its
     *                        type corresponds to this type before the conversion is started, enabling us to just pass
     *                        the current data source and letting this class check that the types correspond.
     */
    public AbstractDataSourceConverter(DataSource destination, DataSourceType destinationType) {
        this.destination = destination;
        this.destinationType = destinationType;
    }

    // Implementation note: Because of ForceFlatToSqlite it is possible that the CommandSender is null,
    // which is never the case when a converter is launched from the /authme converter command.
    @Override
    public void execute(CommandSender sender) {
        if (destinationType != destination.getType()) {
            if (sender != null) {
                sender.sendMessage("Please configure your connection to "
                    + destinationType + " and re-run this command");
            }
            return;
        }

        S source;
        try {
            source = getSource();
        } catch (Exception e) {
            logAndSendMessage(sender, "The data source to convert from could not be initialized");
            logger.logException("Could not initialize source:", e);
            return;
        }

        List<String> skippedPlayers = new ArrayList<>();
        for (PlayerAuth auth : source.getAllAuths()) {
            if (destination.isAuthAvailable(auth.getNickname())) {
                skippedPlayers.add(auth.getNickname());
            } else {
                destination.saveAuth(auth);
                destination.updateSession(auth);
                destination.updateQuitLoc(auth);
            }
        }

        if (!skippedPlayers.isEmpty()) {
            logAndSendMessage(sender, "Skipped conversion for players which were already in "
                + destinationType + ": " + String.join(", ", skippedPlayers));
        }
        logAndSendMessage(sender, "Database successfully converted from " + source.getType()
            + " to " + destinationType);
    }

    /**
     * @return the data source to convert from
     * @throws Exception during initialization of source
     */
    protected abstract S getSource() throws Exception;
}
