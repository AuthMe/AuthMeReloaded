package fr.xephi.authme.datasource.sqlcolumns;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.Columns;

import java.util.function.Function;

/**
 * Columns for the AuthMe data source.
 */
public final class AuthMeColumns {

    public static final PlayerAuthColumn<String> NAME = authCol(
        Type.STRING, c -> c.NAME, false, PlayerAuth::getNickname);

    public static final PlayerAuthColumn<String> REALNAME = authCol(
        Type.STRING, c -> c.REAL_NAME, false, PlayerAuth::getRealName);

    public static final PlayerAuthColumn<String> PASSWORD_HASH = authCol(
        Type.STRING, c -> c.PASSWORD, false, auth -> auth.getPassword().getHash());

    public static final PlayerAuthColumn<String> PASSWORD_SALT = authCol(
        Type.STRING, c -> c.SALT, true, auth -> auth.getPassword().getSalt());

    public static final PlayerAuthColumn<String> EMAIL = authCol(
        Type.STRING, c -> c.EMAIL, false, PlayerAuth::getEmail);

    public static final PlayerAuthColumn<String> LAST_IP = authCol(
        Type.STRING, c -> c.LAST_IP, false, PlayerAuth::getLastIp);

    public static final PlayerAuthColumn<Integer> GROUP_ID = authCol(
        Type.INTEGER, c -> c.GROUP, true, PlayerAuth::getGroupId);

    public static final PlayerAuthColumn<Long> LAST_LOGIN = authCol(
        Type.LONG, c -> c.LAST_LOGIN, false, PlayerAuth::getLastLogin);

    public static final PlayerAuthColumn<String> REGISTRATION_IP = authCol(
        Type.STRING, c -> c.REGISTRATION_IP, false, PlayerAuth::getRegistrationIp);

    public static final PlayerAuthColumn<Long> REGISTRATION_DATE = authCol(
        Type.LONG, c -> c.REGISTRATION_DATE, false, PlayerAuth::getRegistrationDate);

    public static final AuthMeColumn<Boolean> IS_LOGGED_IN = col(
        Type.BOOLEAN, c -> c.IS_LOGGED, true);

    public static final AuthMeColumn<Boolean> HAS_SESSION = col(
        Type.BOOLEAN, c -> c.HAS_SESSION, true);


    private AuthMeColumns() {
    }

    private static <T> AuthMeColumn<T> col(Type<T> type, Function<Columns, String> nameGetter, boolean isOptional) {
        return new AuthMeColumn<>(type, nameGetter, isOptional);
    }

    private static <T> PlayerAuthColumn<T> authCol(Type<T> type, Function<Columns, String> nameGetter,
                                                   boolean isOptional, Function<PlayerAuth, T> playerAuthGetter) {
        return new PlayerAuthColumn<>(type, nameGetter, isOptional, playerAuthGetter);
    }

    public static class AuthMeColumn<T> implements Column<T, Columns> {

        private final Type<T> type;
        private final Function<Columns, String> nameGetter;
        private final boolean isOptional;

        private AuthMeColumn(Type<T> type, Function<Columns, String> nameGetter, boolean isOptional) {
            this.type = type;
            this.nameGetter = nameGetter;
            this.isOptional = isOptional;
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
    }

    private static class PlayerAuthColumn<T>
            extends AuthMeColumn<T> implements DependentColumn<T, Columns, PlayerAuth> {

        private final Function<PlayerAuth, T> playerAuthGetter;

        PlayerAuthColumn(Type<T> type, Function<Columns, String> nameGetter,
                         boolean isOptional, Function<PlayerAuth, T> playerAuthGetter) {
            super(type, nameGetter, isOptional);
            this.playerAuthGetter = playerAuthGetter;
        }

        @Override
        public T getValueFromDependent(PlayerAuth auth) {
            return playerAuthGetter.apply(auth);
        }
    }
}
