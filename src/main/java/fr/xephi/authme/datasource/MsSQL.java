package fr.xephi.authme.datasource;

import com.google.common.annotations.VisibleForTesting;
import com.zaxxer.hikari.HikariDataSource;
import fr.xephi.authme.datasource.sqlextensions.SqlExtensionsFactory;
import fr.xephi.authme.settings.Settings;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
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
        ds.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        ds.setJdbcUrl("jdbc:sqlserver://" + host + ":" + port + ";databaseName=" + database);
        ds.addDataSourceProperty("user", username);
        ds.addDataSourceProperty("password", password);
        ds.addDataSourceProperty("database.name", database);
    }

    @Override
    protected void checkTables() throws SQLException {
        try (Connection con = getConnection(); Statement st = con.createStatement()) {
            // Create table with ID column if it doesn't exist
            String sql = "IF object_id('" + tableName + "') IS NOT NULL"
                + "    CREATE TABLE " + tableName
                + " (" + col.ID + " INT CHECK (" + col.ID + " > 0) IDENTITY"
                + ", PRIMARY KEY (" + col.ID + "));";
            //st.executeUpdate(sql);
        }
    }

    @Override
    protected void checkColumns() throws SQLException {
        try (Connection con = getConnection(); Statement st = con.createStatement()) {
            DatabaseMetaData md = con.getMetaData();
            if (isColumnMissing(md, col.NAME)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD " + col.NAME + " NVARCHAR(255) NOT NULL UNIQUE;");
            }

            if (isColumnMissing(md, col.REAL_NAME)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD " + col.REAL_NAME + " NVARCHAR(255) NOT NULL;");
            }

            if (isColumnMissing(md, col.PASSWORD)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD " + col.PASSWORD + " NVARCHAR(255) NOT NULL;");
            }

            if (!col.SALT.isEmpty() && isColumnMissing(md, col.SALT)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD " + col.SALT + " NVARCHAR(255);");
            }

            if (isColumnMissing(md, col.IP)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD " + col.IP + " NVARCHAR(40) NOT NULL;");
            }

            if (isColumnMissing(md, col.LAST_LOGIN)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD " + col.LAST_LOGIN + " BIGINT NOT NULL DEFAULT 0;");
            } else {
                migrateLastLoginColumn(con, md);
            }

            if (isColumnMissing(md, col.LASTLOC_X)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD "
                    + col.LASTLOC_X + " DOUBLE NOT NULL DEFAULT '0.0' , ADD "
                    + col.LASTLOC_Y + " DOUBLE NOT NULL DEFAULT '0.0' , ADD "
                    + col.LASTLOC_Z + " DOUBLE NOT NULL DEFAULT '0.0';");
            } else {
                st.executeUpdate("ALTER TABLE " + tableName + " MODIFY "
                    + col.LASTLOC_X + " DOUBLE NOT NULL DEFAULT '0.0', MODIFY "
                    + col.LASTLOC_Y + " DOUBLE NOT NULL DEFAULT '0.0', MODIFY "
                    + col.LASTLOC_Z + " DOUBLE NOT NULL DEFAULT '0.0';");
            }

            if (isColumnMissing(md, col.LASTLOC_WORLD)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD "
                    + col.LASTLOC_WORLD + " NVARCHAR(255) NOT NULL DEFAULT 'world';");
            }

            if (isColumnMissing(md, col.LASTLOC_YAW)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD "
                    + col.LASTLOC_YAW + " FLOAT;");
            }

            if (isColumnMissing(md, col.LASTLOC_PITCH)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD "
                    + col.LASTLOC_PITCH + " FLOAT;");
            }

            if (isColumnMissing(md, col.EMAIL)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD "
                    + col.EMAIL + " NVARCHAR(255) DEFAULT 'your@email.com';");
            }

            if (isColumnMissing(md, col.IS_LOGGED)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD "
                    + col.IS_LOGGED + " SMALLINT NOT NULL DEFAULT '0';");
            }
        }
    }

    @Override
    public DataSourceType getType() {
        return DataSourceType.MSSQL;
    }
}
