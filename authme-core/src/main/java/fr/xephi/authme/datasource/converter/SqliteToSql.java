package fr.xephi.authme.datasource.converter;

import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.DataSourceType;
import fr.xephi.authme.datasource.SQLite;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.settings.Settings;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.io.File;
import java.sql.SQLException;

import static fr.xephi.authme.util.Utils.logAndSendMessage;

/**
 * Converts from SQLite to a SQL database (MySQL, MariaDB, or PostgreSQL).
 */
public class SqliteToSql extends AbstractDataSourceConverter<SQLite> {

    private final Settings settings;
    private final File dataFolder;
    private final DataSourceType destinationType;

    @Inject
    SqliteToSql(Settings settings, DataSource dataSource, @DataFolder File dataFolder) {
        super(dataSource, dataSource.getType());
        this.settings = settings;
        this.dataFolder = dataFolder;
        this.destinationType = dataSource.getType();
    }

    @Override
    public void execute(CommandSender sender) {
        if (destinationType == DataSourceType.SQLITE) {
            logAndSendMessage(sender,
                "sqlitetosql requires a MySQL, MariaDB, or PostgreSQL destination — configure the connection in AuthMe's config.yml first.");
            return;
        }
        super.execute(sender);
    }

    @Override
    protected SQLite getSource() throws SQLException {
        return new SQLite(settings, dataFolder);
    }
}
