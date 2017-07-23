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
 * MySQL extensions for Wordpress.
 */
class WordpressExtension extends MySqlExtension {

    private final Columns col;
    private final String tableName;
    private final String wordpressPrefix;

    WordpressExtension(Settings settings, Columns col) {
        this.col = col;
        this.tableName = settings.getProperty(DatabaseSettings.MYSQL_TABLE);
        this.wordpressPrefix = settings.getProperty(HooksSettings.WORDPRESS_TABLE_PREFIX);
    }

    @Override
    public void saveAuth(PlayerAuth auth, Connection con) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement("SELECT " + col.ID + " FROM " + tableName + " WHERE " + col.NAME + "=?;")) {
            pst.setString(1, auth.getNickname());
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(col.ID);
                    String sql = "INSERT INTO " + wordpressPrefix + "usermeta (user_id, meta_key, meta_value) VALUES (?,?,?)";
                    try (PreparedStatement pst2 = con.prepareStatement(sql)) {
                        // First Name
                        pst2.setInt(1, id);
                        pst2.setString(2, "first_name");
                        pst2.setString(3, "");
                        pst2.addBatch();
                        // Last Name
                        pst2.setInt(1, id);
                        pst2.setString(2, "last_name");
                        pst2.setString(3, "");
                        pst2.addBatch();
                        // Nick Name
                        pst2.setInt(1, id);
                        pst2.setString(2, "nickname");
                        pst2.setString(3, auth.getNickname());
                        pst2.addBatch();
                        // Description
                        pst2.setInt(1, id);
                        pst2.setString(2, "description");
                        pst2.setString(3, "");
                        pst2.addBatch();
                        // Rich_Editing
                        pst2.setInt(1, id);
                        pst2.setString(2, "rich_editing");
                        pst2.setString(3, "true");
                        pst2.addBatch();
                        // Comments_Shortcuts
                        pst2.setInt(1, id);
                        pst2.setString(2, "comment_shortcuts");
                        pst2.setString(3, "false");
                        pst2.addBatch();
                        // admin_color
                        pst2.setInt(1, id);
                        pst2.setString(2, "admin_color");
                        pst2.setString(3, "fresh");
                        pst2.addBatch();
                        // use_ssl
                        pst2.setInt(1, id);
                        pst2.setString(2, "use_ssl");
                        pst2.setString(3, "0");
                        pst2.addBatch();
                        // show_admin_bar_front
                        pst2.setInt(1, id);
                        pst2.setString(2, "show_admin_bar_front");
                        pst2.setString(3, "true");
                        pst2.addBatch();
                        // wp_capabilities
                        pst2.setInt(1, id);
                        pst2.setString(2, wordpressPrefix + "capabilities");
                        pst2.setString(3, "a:1:{s:10:\"subscriber\";b:1;}");
                        pst2.addBatch();
                        // wp_user_level
                        pst2.setInt(1, id);
                        pst2.setString(2, wordpressPrefix + "user_level");
                        pst2.setString(3, "0");
                        pst2.addBatch();
                        // default_password_nag
                        pst2.setInt(1, id);
                        pst2.setString(2, "default_password_nag");
                        pst2.setString(3, "");
                        pst2.addBatch();

                        // Execute queries
                        pst2.executeBatch();
                        pst2.clearBatch();
                    }
                }
            }
        }
    }
}
