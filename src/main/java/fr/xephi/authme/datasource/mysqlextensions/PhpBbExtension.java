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
 * Extensions for phpBB when MySQL is used as data source.
 */
class PhpBbExtension extends MySqlExtension {

    private final Columns col;
    private final String tableName;
    private final String phpBbPrefix;
    private final int phpBbGroup;

    PhpBbExtension(Settings settings, Columns col) {
        this.col = col;
        this.tableName = settings.getProperty(DatabaseSettings.MYSQL_TABLE);
        this.phpBbPrefix = settings.getProperty(HooksSettings.PHPBB_TABLE_PREFIX);
        this.phpBbGroup = settings.getProperty(HooksSettings.PHPBB_ACTIVATED_GROUP_ID);
    }

    @Override
    public void saveAuth(PlayerAuth auth, Connection con) throws SQLException {
        String sql = "SELECT " + col.ID + " FROM " + tableName + " WHERE " + col.NAME + "=?;";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, auth.getNickname());
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(col.ID);
                    updateSpecificsOnSave(id, auth.getNickname(), con);
                }
            }
        }
    }

    private void updateSpecificsOnSave(int id, String name, Connection con) throws SQLException {
        // Insert player in phpbb_user_group
        String sql = "INSERT INTO " + phpBbPrefix
                   + "user_group (group_id, user_id, group_leader, user_pending) VALUES (?,?,?,?);";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, phpBbGroup);
            pst.setInt(2, id);
            pst.setInt(3, 0);
            pst.setInt(4, 0);
            pst.executeUpdate();
        }
        // Update username_clean in phpbb_users
        sql = "UPDATE " + tableName + " SET " + tableName + ".username_clean=? WHERE " + col.NAME + "=?;";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, name);
            pst.setString(2, name);
            pst.executeUpdate();
        }
        // Update player group in phpbb_users
        sql = "UPDATE " + tableName + " SET " + tableName + ".group_id=? WHERE " + col.NAME + "=?;";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, phpBbGroup);
            pst.setString(2, name);
            pst.executeUpdate();
        }
        // Get current time without ms
        long time = System.currentTimeMillis() / 1000;
        // Update user_regdate
        sql = "UPDATE " + tableName + " SET " + tableName + ".user_regdate=? WHERE " + col.NAME + "=?;";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setLong(1, time);
            pst.setString(2, name);
            pst.executeUpdate();
        }
        // Update user_lastvisit
        sql = "UPDATE " + tableName + " SET " + tableName + ".user_lastvisit=? WHERE " + col.NAME + "=?;";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setLong(1, time);
            pst.setString(2, name);
            pst.executeUpdate();
        }
        // Increment num_users
        sql = "UPDATE " + phpBbPrefix
            + "config SET config_value = config_value + 1 WHERE config_name = 'num_users';";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.executeUpdate();
        }
    }
}
