package fr.xephi.authme.datasource;

import com.google.common.annotations.VisibleForTesting;
import com.zaxxer.hikari.HikariDataSource;
import fr.xephi.authme.datasource.sqlextensions.SqlExtensionsFactory;
import fr.xephi.authme.settings.Settings;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class MsSQL extends SqlDataSource {

    public MsSQL(Settings settings, SqlExtensionsFactory extensionsFactory) throws SQLException {
        super(settings, extensionsFactory);
    }

    @VisibleForTesting
    MsSQL(Settings settings, HikariDataSource hikariDataSource, SqlExtensionsFactory extensionsFactory) {
        super(settings, hikariDataSource, extensionsFactory);
    }

    @Override
    protected void setConnectionArguments() {
        super.setConnectionArguments();

        // Database URL
        ds.setDataSourceClassName("com.microsoft.sqlserver.jdbc.SQLServerDataSource");
        ds.addDataSourceProperty("serverName", host);
        ds.addDataSourceProperty("port", port);
        ds.addDataSourceProperty("databaseName", database);

        // Auth
        ds.addDataSourceProperty("user", username);
        ds.addDataSourceProperty("password", password);
    }

    @Override
    protected void checkTables() throws SQLException {
        try (Connection con = getConnection(); Statement st = con.createStatement()) {
            // Create table with ID column if it doesn't exist
            String sql = "IF object_id('" + tableName + "') IS NOT NULL\n"
                + "    CREATE TABLE " + tableName
                + " (" + col.ID + " INT CHECK (" + col.ID + " > 0) IDENTITY"
                + ", PRIMARY KEY (" + col.ID + ")) CHARACTER SET = utf8;";
            st.executeUpdate(sql);
        }
    }

    @Override
    public DataSourceType getType() {
        return DataSourceType.MSSQL;
    }
}
