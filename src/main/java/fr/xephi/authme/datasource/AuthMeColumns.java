package fr.xephi.authme.datasource;

import ch.jalu.configme.properties.Property;
import ch.jalu.datasourcecolumns.ColumnType;
import ch.jalu.datasourcecolumns.DependentColumn;
import ch.jalu.datasourcecolumns.StandardTypes;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.settings.properties.DatabaseSettings;

import java.util.function.Function;

public final class AuthMeColumns<T> implements DependentColumn<T, ColumnContext, PlayerAuth> {

    public static final AuthMeColumns<String> NAME = createString(
        DatabaseSettings.MYSQL_COL_NAME, PlayerAuth::getNickname);

    public static final AuthMeColumns<String> NICK_NAME = createString(
        DatabaseSettings.MYSQL_COL_REALNAME, PlayerAuth::getRealName);

    public static final AuthMeColumns<String> PASSWORD = createString(
        DatabaseSettings.MYSQL_COL_PASSWORD, auth -> auth.getPassword().getHash());

    public static final AuthMeColumns<String> SALT = new AuthMeColumns<>(
        StandardTypes.STRING, DatabaseSettings.MYSQL_COL_SALT, auth -> auth.getPassword().getSalt(), true);

    public static final AuthMeColumns<String> EMAIL = createString(
        DatabaseSettings.MYSQL_COL_EMAIL, PlayerAuth::getEmail);

    public static final AuthMeColumns<String> LAST_IP = createString(
        DatabaseSettings.MYSQL_COL_LAST_IP, PlayerAuth::getLastIp);

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

    private AuthMeColumns(ColumnType<T> type, Property<String> nameProperty, Function<PlayerAuth, T> playerAuthGetter,
                          boolean isOptional) {
        this.columnType = type;
        this.nameProperty = nameProperty;
        this.playerAuthGetter = playerAuthGetter;
        this.isOptional = isOptional;
    }

    private static AuthMeColumns<String> createString(Property<String> nameProperty,
                                                      Function<PlayerAuth, String> getter) {
        return new AuthMeColumns<>(StandardTypes.STRING, nameProperty, getter, false);
    }

    private static AuthMeColumns<Double> createDouble(Property<String> nameProperty,
                                                      Function<PlayerAuth, Double> getter) {
        return new AuthMeColumns<>(new DoubleType(), nameProperty, getter, false);
    }

    private static AuthMeColumns<Float> createFloat(Property<String> nameProperty,
                                                    Function<PlayerAuth, Float> getter) {
        return new AuthMeColumns<>(new FloatType(), nameProperty, getter, false);
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
        return false;
    }

    // TODO: Move this to the project...
    private static final class DoubleType implements ColumnType<Double> {

        @Override
        public Class<Double> getClazz() {
            return Double.class;
        }
    }

    private static final class FloatType implements ColumnType<Float> {

        @Override
        public Class<Float> getClazz() {
            return Float.class;
        }
    }
}
