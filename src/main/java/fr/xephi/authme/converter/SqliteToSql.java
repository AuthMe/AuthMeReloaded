package fr.xephi.authme.converter;

import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.DataSourceType;
import fr.xephi.authme.datasource.SQLite;
import fr.xephi.authme.settings.Settings;

import javax.inject.Inject;
import java.sql.SQLException;

/**
 * Converts from SQLite to MySQL.
 */
public class SqliteToSql extends AbstractDataSourceConverter<SQLite> {

    private final Settings settings;

    @Inject
    SqliteToSql(Settings settings, DataSource dataSource) {
        super(dataSource, DataSourceType.MYSQL);
        this.settings = settings;
    }

    @Override
    protected SQLite getSource() throws SQLException, ClassNotFoundException {
        return new SQLite(settings);
    }
}
