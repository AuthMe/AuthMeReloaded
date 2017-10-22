package fr.xephi.authme.datasource.sqlcolumns;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.Columns;

import java.util.function.Function;


public final class Column<T> {

    public static final Column<String> NAME = col(
        Type.STRING, c -> c.NAME, false, PlayerAuth::getNickname);

    public static final Column<String> REALNAME = col(
        Type.STRING, c -> c.REAL_NAME, false, PlayerAuth::getRealName);

    public static final Column<String> PASSWORD_HASH = null;

    public static final Column<String> PASSWORD_SALT = null;

    public static final Column<String> EMAIL = col(
        Type.STRING, c -> c.EMAIL, false, PlayerAuth::getEmail);

    public static final Column<String> LAST_IP = col(
        Type.STRING, c -> c.LAST_IP, false, PlayerAuth::getLastIp);

    public static final Column<Long> REGISTRATION_DATE = col(
        Type.LONG, c -> c.REGISTRATION_DATE, false, PlayerAuth::getLastLogin);

    public static final Column<String> REGISTRATION_IP = null;


    private final Type<T> type;
    private final Function<Columns, String> nameGetter;
    private final boolean isOptional;
    private final Function<PlayerAuth, T> playerAuthGetter;

    private Column(Type<T> type, Function<Columns, String> nameGetter,
                   boolean isOptional, Function<PlayerAuth, T> playerAuthGetter) {
        this.type = type;
        this.nameGetter = nameGetter;
        this.isOptional = isOptional;
        this.playerAuthGetter = playerAuthGetter;
    }

    private static <T> Column<T> col(Type<T> type, Function<Columns, String> nameGetter,
                                     boolean isOptional, Function<PlayerAuth, T> playerAuthGetter) {
        return new Column<>(type, nameGetter, isOptional, playerAuthGetter);
    }

    public String returnName(Columns col) {
        return nameGetter.apply(col);
    }

    public Type<T> getType() {
        return type;
    }

    public boolean isColumnUsed(Columns col) {
        return isOptional && !returnName(col).isEmpty();
    }

    public T fromPlayerAuth(PlayerAuth auth) {
        return playerAuthGetter.apply(auth);
    }
}
