package fr.xephi.authme.datasource.mysqlextensions;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.Columns;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import fr.xephi.authme.settings.properties.HooksSettings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Extension for IPB4.
 */
class Ipb4Extension extends MySqlExtension {

    private final Columns col;
    private final String tableName;
    private final String ipbPrefix;
    private final int ipbGroup;

    Ipb4Extension(Settings settings, Columns col) {
        this.col = col;
        this.tableName = settings.getProperty(DatabaseSettings.MYSQL_TABLE);
        this.ipbPrefix = settings.getProperty(HooksSettings.IPB_TABLE_PREFIX);
        this.ipbGroup = settings.getProperty(HooksSettings.IPB_ACTIVATED_GROUP_ID);
    }

    @Override
    public void saveAuth(PlayerAuth auth, Connection con) throws SQLException {
        String sql = "SELECT " + col.ID + " FROM " + tableName + " WHERE " + col.NAME + "=?;";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, auth.getNickname());
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    // Update player group in core_members
                    sql = "UPDATE " + ipbPrefix + tableName
                        + " SET " + tableName + ".member_group_id=? WHERE " + col.NAME + "=?;";
                    try (PreparedStatement pst2 = con.prepareStatement(sql)) {
                        pst2.setInt(1, ipbGroup);
                        pst2.setString(2, auth.getNickname());
                        pst2.executeUpdate();
                    }
                    // Get current time without ms
                    long time = System.currentTimeMillis() / 1000;
                    // update joined date
                    sql = "UPDATE " + ipbPrefix + tableName
                        + " SET " + tableName + ".joined=? WHERE " + col.NAME + "=?;";
                    try (PreparedStatement pst2 = con.prepareStatement(sql)) {
                        pst2.setLong(1, time);
                        pst2.setString(2, auth.getNickname());
                        pst2.executeUpdate();
                    }
                    // Update last_visit
                    sql = "UPDATE " + ipbPrefix + tableName
                        + " SET " + tableName + ".last_visit=? WHERE " + col.NAME + "=?;";
                    try (PreparedStatement pst2 = con.prepareStatement(sql)) {
                        pst2.setLong(1, time);
                        pst2.setString(2, auth.getNickname());
                        pst2.executeUpdate();
                    }
                }
            }
        }
    }
}
