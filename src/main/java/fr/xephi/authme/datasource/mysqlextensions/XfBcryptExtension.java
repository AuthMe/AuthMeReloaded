package fr.xephi.authme.datasource.mysqlextensions;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.Columns;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.security.crypts.XfBCrypt;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import fr.xephi.authme.settings.properties.HooksSettings;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Extension for XFBCRYPT.
 */
class XfBcryptExtension extends MySqlExtension {
    
    private final Columns col;
    private final String tableName;
    private final String xfPrefix;
    private final int xfGroup;
    
    XfBcryptExtension(Settings settings, Columns col) {
        this.col = col;
        this.tableName = settings.getProperty(DatabaseSettings.MYSQL_TABLE);
        this.xfPrefix = settings.getProperty(HooksSettings.XF_TABLE_PREFIX);
        this.xfGroup = settings.getProperty(HooksSettings.XF_ACTIVATED_GROUP_ID);
    }

    @Override
    public void saveAuth(PlayerAuth auth, Connection con) throws SQLException {
        String sql = "SELECT " + col.ID + " FROM " + tableName + " WHERE " + col.NAME + "=?;";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, auth.getNickname());
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(col.ID);
                    updateXenforoTablesOnSave(auth, id, con);
                }
            }
        }
    }
    
    private void updateXenforoTablesOnSave(PlayerAuth auth, int id, Connection con) throws SQLException {
        // Insert player password, salt in xf_user_authenticate
        String sql = "INSERT INTO " + xfPrefix + "user_authenticate (user_id, scheme_class, data) VALUES (?,?,?)";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, id);
            pst.setString(2, XfBCrypt.SCHEME_CLASS);
            String serializedHash = XfBCrypt.serializeHash(auth.getPassword().getHash());
            byte[] bytes = serializedHash.getBytes();
            Blob blob = con.createBlob();
            blob.setBytes(1, bytes);
            pst.setBlob(3, blob);
            pst.executeUpdate();
        }
        // Update player group in xf_users
        sql = "UPDATE " + tableName + " SET " + tableName + ".user_group_id=? WHERE " + col.NAME + "=?;";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, xfGroup);
            pst.setString(2, auth.getNickname());
            pst.executeUpdate();
        }
        // Update player permission combination in xf_users
        sql = "UPDATE " + tableName + " SET " + tableName + ".permission_combination_id=? WHERE " + col.NAME + "=?;";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, xfGroup);
            pst.setString(2, auth.getNickname());
            pst.executeUpdate();
        }
        // Insert player privacy combination in xf_user_privacy
        sql = "INSERT INTO " + xfPrefix + "user_privacy (user_id, allow_view_profile, allow_post_profile, "
            + "allow_send_personal_conversation, allow_view_identities, allow_receive_news_feed) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, id);
            pst.setString(2, "everyone");
            pst.setString(3, "members");
            pst.setString(4, "members");
            pst.setString(5, "everyone");
            pst.setString(6, "everyone");
            pst.executeUpdate();
        }
        // Insert player group relation in xf_user_group_relation
        sql = "INSERT INTO " + xfPrefix + "user_group_relation (user_id, user_group_id, is_primary) VALUES (?,?,?)";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, id);
            pst.setInt(2, xfGroup);
            pst.setString(3, "1");
            pst.executeUpdate();
        }
    }

    public void extendAuth(PlayerAuth auth, int id, Connection con) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement(
            "SELECT data FROM " + xfPrefix + "user_authenticate WHERE " + col.ID + "=?;")) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    Blob blob = rs.getBlob("data");
                    byte[] bytes = blob.getBytes(1, (int) blob.length());
                    auth.setPassword(new HashedPassword(XfBCrypt.getHashFromBlob(bytes)));
                }
            }
        }
    }

    @Override
    public void changePassword(String user, HashedPassword password, Connection con) throws SQLException {
        String sql = "SELECT " + col.ID + " FROM " + tableName + " WHERE " + col.NAME + "=?;";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, user);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(col.ID);
                    // Insert password in the correct table
                    // TODO #1255: Close these statements with try-catch
                    sql = "UPDATE " + xfPrefix + "user_authenticate SET data=? WHERE " + col.ID + "=?;";
                    PreparedStatement pst2 = con.prepareStatement(sql);
                    String serializedHash = XfBCrypt.serializeHash(password.getHash());
                    byte[] bytes = serializedHash.getBytes();
                    Blob blob = con.createBlob();
                    blob.setBytes(1, bytes);
                    pst2.setBlob(1, blob);
                    pst2.setInt(2, id);
                    pst2.executeUpdate();
                    pst2.close();
                    // ...
                    sql = "UPDATE " + xfPrefix + "user_authenticate SET scheme_class=? WHERE " + col.ID + "=?;";
                    pst2 = con.prepareStatement(sql);
                    pst2.setString(1, XfBCrypt.SCHEME_CLASS);
                    pst2.setInt(2, id);
                    pst2.executeUpdate();
                    pst2.close();
                }
            }
        }
    }

    @Override
    public void removeAuth(String user, Connection con) throws SQLException {
        String sql = "SELECT " + col.ID + " FROM " + tableName + " WHERE " + col.NAME + "=?;";
        try (PreparedStatement xfSelect = con.prepareStatement(sql)) {
            xfSelect.setString(1, user);
            try (ResultSet rs = xfSelect.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(col.ID);
                    sql = "DELETE FROM " + xfPrefix + "user_authenticate WHERE " + col.ID + "=?;";
                    try (PreparedStatement xfDelete = con.prepareStatement(sql)) {
                        xfDelete.setInt(1, id);
                        xfDelete.executeUpdate();
                    }
                }
            }
        }
    }
}
