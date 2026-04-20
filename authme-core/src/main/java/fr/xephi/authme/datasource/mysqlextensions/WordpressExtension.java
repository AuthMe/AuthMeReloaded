package fr.xephi.authme.datasource.mysqlextensions;

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
class WordpressExtension extends MySqlExtension {

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

    /**
     * Saves the required data to Wordpress tables.
     *
     * @param auth the player data
     * @param id the player id
     * @param con the sql connection
     * @throws SQLException .
     */
    private void saveSpecifics(PlayerAuth auth, int id, Connection con) throws SQLException {
        String sql = "INSERT INTO " + wordpressPrefix + "usermeta (user_id, meta_key, meta_value) VALUES (?,?,?)";
        try (PreparedStatement pst = con.prepareStatement(sql)) {

            new UserMetaBatchAdder(pst, id)
                .addMetaRow("first_name", "")
                .addMetaRow("last_name", "")
                .addMetaRow("nickname", auth.getNickname())
                .addMetaRow("description", "")
                .addMetaRow("rich_editing", "true")
                .addMetaRow("comment_shortcuts", "false")
                .addMetaRow("admin_color", "fresh")
                .addMetaRow("use_ssl", "0")
                .addMetaRow("show_admin_bar_front", "true")
                .addMetaRow(wordpressPrefix + "capabilities", "a:1:{s:10:\"subscriber\";b:1;}")
                .addMetaRow(wordpressPrefix + "user_level", "0")
                .addMetaRow("default_password_nag", "");

            // Execute queries
            pst.executeBatch();
            pst.clearBatch();
        }
    }

    /** Helper to add batch entries to the wrapped prepared statement. */
    private static final class UserMetaBatchAdder {

        private final PreparedStatement pst;
        private final int userId;

        UserMetaBatchAdder(PreparedStatement pst, int userId) {
            this.pst = pst;
            this.userId = userId;
        }

        UserMetaBatchAdder addMetaRow(String metaKey, String metaValue) throws SQLException {
            pst.setInt(1, userId);
            pst.setString(2, metaKey);
            pst.setString(3, metaValue);
            pst.addBatch();
            return this;
        }
    }
}
