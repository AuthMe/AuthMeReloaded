package fr.xephi.authme.datasource.columnshandler;

import ch.jalu.configme.properties.Property;
import ch.jalu.datasourcecolumns.ColumnType;
import ch.jalu.datasourcecolumns.DependentColumn;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.settings.properties.DatabaseSettings;

import java.util.function.Function;

import static fr.xephi.authme.datasource.columnshandler.AuthMeColumnsFactory.ColumnOptions.DEFAULT_FOR_NULL;
import static fr.xephi.authme.datasource.columnshandler.AuthMeColumnsFactory.ColumnOptions.OPTIONAL;
import static fr.xephi.authme.datasource.columnshandler.AuthMeColumnsFactory.createDouble;
import static fr.xephi.authme.datasource.columnshandler.AuthMeColumnsFactory.createFloat;
import static fr.xephi.authme.datasource.columnshandler.AuthMeColumnsFactory.createInteger;
import static fr.xephi.authme.datasource.columnshandler.AuthMeColumnsFactory.createLong;
import static fr.xephi.authme.datasource.columnshandler.AuthMeColumnsFactory.createString;

/**
 * Column definitions for the AuthMe table.
 *
 * @param <T> the column type
 * @see PlayerAuth
 */
public final class AuthMeColumns<T> implements DependentColumn<T, ColumnContext, PlayerAuth> {

    public static final AuthMeColumns<String> NAME = createString(
        DatabaseSettings.MYSQL_COL_NAME, PlayerAuth::getNickname);

    public static final AuthMeColumns<String> NICK_NAME = createString(
        DatabaseSettings.MYSQL_COL_REALNAME, PlayerAuth::getRealName);

    public static final AuthMeColumns<String> PASSWORD = createString(
        DatabaseSettings.MYSQL_COL_PASSWORD, auth -> auth.getPassword().getHash());

    public static final AuthMeColumns<String> SALT = createString(
        DatabaseSettings.MYSQL_COL_SALT, auth -> auth.getPassword().getSalt(), OPTIONAL);

    public static final AuthMeColumns<String> EMAIL = createString(
        DatabaseSettings.MYSQL_COL_EMAIL, PlayerAuth::getEmail, DEFAULT_FOR_NULL);

    public static final AuthMeColumns<String> LAST_IP = createString(
        DatabaseSettings.MYSQL_COL_LAST_IP, PlayerAuth::getLastIp);

    public static final AuthMeColumns<Integer> GROUP_ID = createInteger(
        DatabaseSettings.MYSQL_COL_GROUP, PlayerAuth::getGroupId, OPTIONAL);

    public static final AuthMeColumns<Long> LAST_LOGIN = createLong(
        DatabaseSettings.MYSQL_COL_LASTLOGIN, PlayerAuth::getLastLogin);

    public static final AuthMeColumns<String> REGISTRATION_IP = createString(
        DatabaseSettings.MYSQL_COL_REGISTER_IP, PlayerAuth::getRegistrationIp);

    public static final AuthMeColumns<Long> REGISTRATION_DATE = createLong(
        DatabaseSettings.MYSQL_COL_REGISTER_DATE, PlayerAuth::getRegistrationDate);

    public static final AuthMeColumns<Double> LOCATION_X = createDouble(
        DatabaseSettings.MYSQL_COL_LASTLOC_X, PlayerAuth::getQuitLocX);

    public static final AuthMeColumns<Double> LOCATION_Y = createDouble(
        DatabaseSettings.MYSQL_COL_LASTLOC_Y, PlayerAuth::getQuitLocY);

    public static final AuthMeColumns<Double> LOCATION_Z = createDouble(
        DatabaseSettings.MYSQL_COL_LASTLOC_Z, PlayerAuth::getQuitLocZ);

    public static final AuthMeColumns<String> LOCATION_WORLD = createString(
        DatabaseSettings.MYSQL_COL_LASTLOC_WORLD, PlayerAuth::getWorld);

    public static final AuthMeColumns<Float> LOCATION_YAW = createFloat(
        DatabaseSettings.MYSQL_COL_LASTLOC_YAW, PlayerAuth::getYaw);

    public static final AuthMeColumns<Float> LOCATION_PITCH = createFloat(
        DatabaseSettings.MYSQL_COL_LASTLOC_PITCH, PlayerAuth::getPitch);


    private final ColumnType<T> columnType;
    private final Property<String> nameProperty;
    private final Function<PlayerAuth, T> playerAuthGetter;
    private final boolean isOptional;
    private final boolean useDefaultForNull;

    AuthMeColumns(ColumnType<T> type, Property<String> nameProperty, Function<PlayerAuth, T> playerAuthGetter,
                  boolean isOptional, boolean useDefaultForNull) {
        this.columnType = type;
        this.nameProperty = nameProperty;
        this.playerAuthGetter = playerAuthGetter;
        this.isOptional = isOptional;
        this.useDefaultForNull = useDefaultForNull;
    }


    public Property<String> getNameProperty() {
        return nameProperty;
    }

    @Override
    public T getValueFromDependent(PlayerAuth playerAuth) {
        return playerAuthGetter.apply(playerAuth);
    }

    @Override
    public String resolveName(ColumnContext columnContext) {
        return columnContext.getName(this);
    }

    @Override
    public ColumnType<T> getType() {
        return columnType;
    }

    @Override
    public boolean isColumnUsed(ColumnContext columnContext) {
        return !isOptional || !resolveName(columnContext).isEmpty();
    }

    @Override
    public boolean useDefaultForNullValue(ColumnContext columnContext) {
        return useDefaultForNull && columnContext.hasDefaultSupport();
    }
}
