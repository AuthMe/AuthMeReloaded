package fr.xephi.authme.datasource.columnshandler;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.settings.properties.DatabaseSettings;

import static fr.xephi.authme.datasource.columnshandler.AuthMeColumnsFactory.ColumnOptions.DEFAULT_FOR_NULL;
import static fr.xephi.authme.datasource.columnshandler.AuthMeColumnsFactory.ColumnOptions.OPTIONAL;
import static fr.xephi.authme.datasource.columnshandler.AuthMeColumnsFactory.createDouble;
import static fr.xephi.authme.datasource.columnshandler.AuthMeColumnsFactory.createFloat;
import static fr.xephi.authme.datasource.columnshandler.AuthMeColumnsFactory.createInteger;
import static fr.xephi.authme.datasource.columnshandler.AuthMeColumnsFactory.createLong;
import static fr.xephi.authme.datasource.columnshandler.AuthMeColumnsFactory.createString;

/**
 * Contains column definitions for the AuthMe table.
 */
public final class AuthMeColumns {

    public static final PlayerAuthColumn<String> NAME = createString(
        DatabaseSettings.MYSQL_COL_NAME, PlayerAuth::getNickname);

    public static final PlayerAuthColumn<String> NICK_NAME = createString(
        DatabaseSettings.MYSQL_COL_REALNAME, PlayerAuth::getRealName);

    public static final PlayerAuthColumn<String> PASSWORD = createString(
        DatabaseSettings.MYSQL_COL_PASSWORD, auth -> auth.getPassword().getHash());

    public static final PlayerAuthColumn<String> SALT = createString(
        DatabaseSettings.MYSQL_COL_SALT, auth -> auth.getPassword().getSalt(), OPTIONAL);

    public static final PlayerAuthColumn<String> EMAIL = createString(
        DatabaseSettings.MYSQL_COL_EMAIL, PlayerAuth::getEmail, DEFAULT_FOR_NULL);

    public static final PlayerAuthColumn<String> LAST_IP = createString(
        DatabaseSettings.MYSQL_COL_LAST_IP, PlayerAuth::getLastIp);

    public static final PlayerAuthColumn<Integer> GROUP_ID = createInteger(
        DatabaseSettings.MYSQL_COL_GROUP, PlayerAuth::getGroupId, OPTIONAL);

    public static final PlayerAuthColumn<Long> LAST_LOGIN = createLong(
        DatabaseSettings.MYSQL_COL_LASTLOGIN, PlayerAuth::getLastLogin);

    public static final PlayerAuthColumn<String> REGISTRATION_IP = createString(
        DatabaseSettings.MYSQL_COL_REGISTER_IP, PlayerAuth::getRegistrationIp);

    public static final PlayerAuthColumn<Long> REGISTRATION_DATE = createLong(
        DatabaseSettings.MYSQL_COL_REGISTER_DATE, PlayerAuth::getRegistrationDate);

    // --------
    // Columns not on PlayerAuth
    // --------
    public static final DataSourceColumn<Integer> IS_LOGGED = createInteger(
        DatabaseSettings.MYSQL_COL_ISLOGGED);

    public static final DataSourceColumn<Integer> HAS_SESSION = createInteger(
        DatabaseSettings.MYSQL_COL_HASSESSION);

    private AuthMeColumns() {
    }
}
