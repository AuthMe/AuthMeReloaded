package fr.xephi.authme.datasource;

import com.google.common.annotations.VisibleForTesting;
import com.zaxxer.hikari.HikariDataSource;
import fr.xephi.authme.datasource.sqlextensions.SqlExtensionsFactory;
import fr.xephi.authme.settings.Settings;

import java.sql.SQLException;

public class PostgreSQL extends MySQL {

    public PostgreSQL(Settings settings, SqlExtensionsFactory extensionsFactory) throws SQLException {
        super(settings, extensionsFactory);
    }

    @VisibleForTesting
    PostgreSQL(Settings settings, HikariDataSource hikariDataSource, SqlExtensionsFactory extensionsFactory) {
        super(settings, hikariDataSource, extensionsFactory);
    }

    @Override
    void initHikariDataSource() {

        ds = new HikariDataSource();
        ds.setPoolName("AuthMePostgreSQLPool");

        // Database URL
        ds.setJdbcUrl("jdbc:postgresql://" + this.host + ":" + this.port + "/" + this.database);
    }

    @Override
    public DataSourceType getType() { return DataSourceType.POSTGRESQL; }
}
