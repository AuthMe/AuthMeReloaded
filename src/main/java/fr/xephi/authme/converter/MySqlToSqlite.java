package fr.xephi.authme.converter;

import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.DataSourceType;
import fr.xephi.authme.datasource.MySQL;
import fr.xephi.authme.settings.Settings;

import javax.inject.Inject;
import java.sql.SQLException;

/**
 * Converts from MySQL to SQLite.
 */
public class MySqlToSqlite extends AbstractDataSourceConverter<MySQL> {

    private final Settings settings;

    @Inject
    MySqlToSqlite(DataSource dataSource, Settings settings) {
        super(dataSource, DataSourceType.SQLITE);
        this.settings = settings;
    }

    @Override
    protected MySQL getSource() throws SQLException, ClassNotFoundException {
        return new MySQL(settings);
    }
}
