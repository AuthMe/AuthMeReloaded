package fr.xephi.authme.datasource.sqlcolumns;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.Columns;

import java.util.function.Function;

/**
 * Columns for the AuthMe data source.
 */
public class AuthMeColumns<T> implements DependentColumn<T, Columns, PlayerAuth> {

    public static final AuthMeColumns<String> NAME = col(
        Type.STRING, c -> c.NAME, false, PlayerAuth::getNickname);

    public static final AuthMeColumns<String> REALNAME = col(
        Type.STRING, c -> c.REAL_NAME, false, PlayerAuth::getRealName);

    public static final AuthMeColumns<String> PASSWORD_HASH = null;

    public static final AuthMeColumns<String> PASSWORD_SALT = null;

    public static final AuthMeColumns<String> EMAIL = col(
        Type.STRING, c -> c.EMAIL, false, PlayerAuth::getEmail);

    public static final AuthMeColumns<String> LAST_IP = col(
        Type.STRING, c -> c.LAST_IP, false, PlayerAuth::getLastIp);

    public static final AuthMeColumns<Long> REGISTRATION_DATE = col(
        Type.LONG, c -> c.REGISTRATION_DATE, false, PlayerAuth::getLastLogin);

    public static final AuthMeColumns<String> REGISTRATION_IP = null;


    private final Type<T> type;
    private final Function<Columns, String> nameGetter;
    private final boolean isOptional;
    private final Function<PlayerAuth, T> playerAuthGetter;

    private AuthMeColumns(Type<T> type, Function<Columns, String> nameGetter,
                   boolean isOptional, Function<PlayerAuth, T> playerAuthGetter) {
        this.type = type;
        this.nameGetter = nameGetter;
        this.isOptional = isOptional;
        this.playerAuthGetter = playerAuthGetter;
    }

    private static <T> AuthMeColumns<T> col(Type<T> type, Function<Columns, String> nameGetter,
                                     boolean isOptional, Function<PlayerAuth, T> playerAuthGetter) {
        return new AuthMeColumns<>(type, nameGetter, isOptional, playerAuthGetter);
    }

    @Override
    public String resolveName(Columns col) {
        return nameGetter.apply(col);
    }

    @Override
    public Type<T> getType() {
        return type;
    }

    @Override
    public boolean isColumnUsed(Columns col) {
        return isOptional && !resolveName(col).isEmpty();
    }

    @Override
    public T getFromDependent(PlayerAuth auth) {
        return playerAuthGetter.apply(auth);
    }
}
