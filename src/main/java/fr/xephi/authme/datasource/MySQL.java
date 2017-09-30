package fr.xephi.authme.datasource;

import com.google.common.annotations.VisibleForTesting;
import com.zaxxer.hikari.HikariDataSource;
import fr.xephi.authme.datasource.sqlextensions.SqlExtensionsFactory;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.DatabaseSettings;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;

public class MySQL extends SqlDataSource {

    private boolean useSsl;

    public MySQL(Settings settings, SqlExtensionsFactory extensionsFactory) throws SQLException {
        super(settings, extensionsFactory);
    }

    @VisibleForTesting
    MySQL(Settings settings, HikariDataSource hikariDataSource, SqlExtensionsFactory extensionsFactory) {
        super(settings, hikariDataSource, extensionsFactory);
    }

    @Override
    protected void setParameters(Settings settings, SqlExtensionsFactory extensionsFactory) {
        super.setParameters(settings, extensionsFactory);
        this.useSsl = settings.getProperty(DatabaseSettings.MYSQL_USE_SSL);
    }

    @Override
    protected void setConnectionArguments() {
        super.setConnectionArguments();

        // Database URL
        ds.setJdbcUrl("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database);

        // Auth
        ds.setUsername(this.username);
        ds.setPassword(this.password);

        // Request mysql over SSL
        ds.addDataSourceProperty("useSSL", String.valueOf(useSsl));

        // Encoding
        ds.addDataSourceProperty("characterEncoding", "utf8");
        ds.addDataSourceProperty("encoding", "UTF-8");
        ds.addDataSourceProperty("useUnicode", "true");

        // Random stuff
        ds.addDataSourceProperty("rewriteBatchedStatements", "true");
        ds.addDataSourceProperty("jdbcCompliantTruncation", "false");

        // Caching
        ds.addDataSourceProperty("cachePrepStmts", "true");
        ds.addDataSourceProperty("prepStmtCacheSize", "275");
        ds.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    }

    @Override
    protected void checkTables() throws SQLException {
        try (Connection con = getConnection(); Statement st = con.createStatement()) {
            // Create table with ID column if it doesn't exist
            String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + col.ID + " MEDIUMINT(8) UNSIGNED AUTO_INCREMENT,"
                + "PRIMARY KEY (" + col.ID + ")"
                + ") CHARACTER SET = utf8;";
            st.executeUpdate(sql);
        }
    }

    @Override
    protected void checkColumns() throws SQLException {
        try (Connection con = getConnection(); Statement st = con.createStatement()) {
            DatabaseMetaData md = con.getMetaData();
            if (isColumnMissing(md, col.NAME)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.NAME + " VARCHAR(255) NOT NULL UNIQUE AFTER " + col.ID + ";");
            }

            if (isColumnMissing(md, col.REAL_NAME)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.REAL_NAME + " VARCHAR(255) NOT NULL AFTER " + col.NAME + ";");
            }

            if (isColumnMissing(md, col.PASSWORD)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.PASSWORD + " VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL;");
            }

            if (!col.SALT.isEmpty() && isColumnMissing(md, col.SALT)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + col.SALT + " VARCHAR(255);");
            }

            if (isColumnMissing(md, col.IP)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.IP + " VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin NOT NULL;");
            }

            if (isColumnMissing(md, col.LAST_LOGIN)) {
                st.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + col.LAST_LOGIN + " BIGINT NOT NULL DEFAULT 0;");
            } else {
                migrateLastLoginColumn(con, md);
            }

            if (isColumnMissing(md, col.LASTLOC_X)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + col.LASTLOC_X + " DOUBLE NOT NULL DEFAULT '0.0' AFTER " + col.LAST_LOGIN + " , ADD "
                    + col.LASTLOC_Y + " DOUBLE NOT NULL DEFAULT '0.0' AFTER " + col.LASTLOC_X + " , ADD "
                    + col.LASTLOC_Z + " DOUBLE NOT NULL DEFAULT '0.0' AFTER " + col.LASTLOC_Y);
            } else {
                st.executeUpdate("ALTER TABLE " + tableName + " MODIFY "
                    + col.LASTLOC_X + " DOUBLE NOT NULL DEFAULT '0.0', MODIFY "
                    + col.LASTLOC_Y + " DOUBLE NOT NULL DEFAULT '0.0', MODIFY "
                    + col.LASTLOC_Z + " DOUBLE NOT NULL DEFAULT '0.0';");
            }

            if (isColumnMissing(md, col.LASTLOC_WORLD)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + col.LASTLOC_WORLD + " VARCHAR(255) NOT NULL DEFAULT 'world' AFTER " + col.LASTLOC_Z);
            }

            if (isColumnMissing(md, col.LASTLOC_YAW)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + col.LASTLOC_YAW + " FLOAT;");
            }

            if (isColumnMissing(md, col.LASTLOC_PITCH)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + col.LASTLOC_PITCH + " FLOAT;");
            }

            if (isColumnMissing(md, col.EMAIL)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + col.EMAIL + " VARCHAR(255) DEFAULT 'your@email.com' AFTER " + col.LASTLOC_WORLD);
            }

            if (isColumnMissing(md, col.IS_LOGGED)) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN "
                    + col.IS_LOGGED + " SMALLINT NOT NULL DEFAULT '0' AFTER " + col.EMAIL);
            }
        }
    }

    @Override
    public DataSourceType getType() {
        return DataSourceType.MYSQL;
    }
}
