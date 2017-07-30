package fr.xephi.authme.datasource.sqlextensions;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.Columns;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.HooksSettings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.OptionalInt;

/**
 * MySQL extensions for Wordpress.
 */
class WordpressExtension extends SqlExtension {

    private final String wordpressPrefix;

    WordpressExtension(Settings settings, Columns col) {
        super(settings, col);
        this.wordpressPrefix = settings.getProperty(HooksSettings.WORDPRESS_TABLE_PREFIX);
    }

    @Override
    public void saveAuth(PlayerAuth auth, Connection con) throws SQLException {
        OptionalInt authId = retrieveIdFromTable(auth.getNickname(), con);
        if (authId.isPresent()) {
            saveSpecifics(auth, authId.getAsInt(), con);
        }
    }

    private void saveSpecifics(PlayerAuth auth, int id, Connection con) throws SQLException {
        String sql = "INSERT INTO " + wordpressPrefix + "usermeta (user_id, meta_key, meta_value) VALUES (?,?,?)";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            // First Name
            pst.setInt(1, id);
            pst.setString(2, "first_name");
            pst.setString(3, "");
            pst.addBatch();
            // Last Name
            pst.setInt(1, id);
            pst.setString(2, "last_name");
            pst.setString(3, "");
            pst.addBatch();
            // Nick Name
            pst.setInt(1, id);
            pst.setString(2, "nickname");
            pst.setString(3, auth.getNickname());
            pst.addBatch();
            // Description
            pst.setInt(1, id);
            pst.setString(2, "description");
            pst.setString(3, "");
            pst.addBatch();
            // Rich_Editing
            pst.setInt(1, id);
            pst.setString(2, "rich_editing");
            pst.setString(3, "true");
            pst.addBatch();
            // Comments_Shortcuts
            pst.setInt(1, id);
            pst.setString(2, "comment_shortcuts");
            pst.setString(3, "false");
            pst.addBatch();
            // admin_color
            pst.setInt(1, id);
            pst.setString(2, "admin_color");
            pst.setString(3, "fresh");
            pst.addBatch();
            // use_ssl
            pst.setInt(1, id);
            pst.setString(2, "use_ssl");
            pst.setString(3, "0");
            pst.addBatch();
            // show_admin_bar_front
            pst.setInt(1, id);
            pst.setString(2, "show_admin_bar_front");
            pst.setString(3, "true");
            pst.addBatch();
            // wp_capabilities
            pst.setInt(1, id);
            pst.setString(2, wordpressPrefix + "capabilities");
            pst.setString(3, "a:1:{s:10:\"subscriber\";b:1;}");
            pst.addBatch();
            // wp_user_level
            pst.setInt(1, id);
            pst.setString(2, wordpressPrefix + "user_level");
            pst.setString(3, "0");
            pst.addBatch();
            // default_password_nag
            pst.setInt(1, id);
            pst.setString(2, "default_password_nag");
            pst.setString(3, "");
            pst.addBatch();

            // Execute queries
            pst.executeBatch();
            pst.clearBatch();
        }
    }
}
