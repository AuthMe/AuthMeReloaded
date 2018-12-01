package fr.xephi.authme.datasource;

import ch.jalu.datasourcecolumns.data.DataSourceValue;
import ch.jalu.datasourcecolumns.data.DataSourceValueImpl;
import ch.jalu.datasourcecolumns.data.DataSourceValues;
import ch.jalu.datasourcecolumns.predicate.AlwaysTruePredicate;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.player.NamedIdentifier;
import fr.xephi.authme.datasource.columnshandler.AuthMeColumns;
import fr.xephi.authme.datasource.columnshandler.AuthMeColumnsHandler;
import fr.xephi.authme.security.crypts.HashedPassword;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static ch.jalu.datasourcecolumns.data.UpdateValues.with;
import static ch.jalu.datasourcecolumns.predicate.StandardPredicates.eq;
import static ch.jalu.datasourcecolumns.predicate.StandardPredicates.eqIgnoreCase;
import static fr.xephi.authme.datasource.SqlDataSourceUtils.logSqlException;

/**
 * Common type for SQL-based data sources. Classes implementing this
 * must ensure that {@link #columnsHandler} is initialized on creation.
 */
public abstract class AbstractSqlDataSource implements DataSource {

    protected AuthMeColumnsHandler columnsHandler;

    @Override
    public boolean isAuthAvailable(NamedIdentifier identifier) {
        try {
            return columnsHandler.retrieve(identifier.getLowercaseName(), AuthMeColumns.NAME).rowExists();
        } catch (SQLException e) {
            logSqlException(e);
            return false;
        }
    }

    @Override
    public HashedPassword getPassword(NamedIdentifier identifier) {
        try {
            DataSourceValues values = columnsHandler.retrieve(identifier.getLowercaseName(), AuthMeColumns.PASSWORD, AuthMeColumns.SALT);
            if (values.rowExists()) {
                return new HashedPassword(values.get(AuthMeColumns.PASSWORD), values.get(AuthMeColumns.SALT));
            }
        } catch (SQLException e) {
            logSqlException(e);
        }
        return null;
    }

    @Override
    public boolean saveAuth(PlayerAuth auth) {
        return columnsHandler.insert(auth,
            AuthMeColumns.NAME, AuthMeColumns.NICK_NAME, AuthMeColumns.PASSWORD, AuthMeColumns.SALT,
            AuthMeColumns.EMAIL, AuthMeColumns.REGISTRATION_DATE, AuthMeColumns.REGISTRATION_IP);
    }

    @Override
    public boolean hasSession(NamedIdentifier identifier) {
        try {
            DataSourceValue<Integer> result = columnsHandler.retrieve(identifier.getLowercaseName(), AuthMeColumns.HAS_SESSION);
            return result.rowExists() && Integer.valueOf(1).equals(result.getValue());
        } catch (SQLException e) {
            logSqlException(e);
            return false;
        }
    }

    @Override
    public boolean updateSession(PlayerAuth auth) {
        return columnsHandler.update(auth, AuthMeColumns.LAST_IP, AuthMeColumns.LAST_LOGIN, AuthMeColumns.NICK_NAME);
    }

    @Override
    public boolean updatePassword(PlayerAuth auth) {
        return updatePassword(auth.toIdentifier(), auth.getPassword());
    }

    @Override
    public boolean updatePassword(NamedIdentifier identifier, HashedPassword password) {
        return columnsHandler.update(identifier.getLowercaseName(),
            with(AuthMeColumns.PASSWORD, password.getHash())
            .and(AuthMeColumns.SALT, password.getSalt()).build());
    }

    @Override
    public boolean updateQuitLoc(PlayerAuth auth) {
        return columnsHandler.update(auth,
            AuthMeColumns.LOCATION_X, AuthMeColumns.LOCATION_Y, AuthMeColumns.LOCATION_Z,
            AuthMeColumns.LOCATION_WORLD, AuthMeColumns.LOCATION_YAW, AuthMeColumns.LOCATION_PITCH);
    }

    @Override
    public List<NamedIdentifier> getAllAuthsByIp(String ip) {
        try {
            return columnsHandler.retrieve(eq(AuthMeColumns.LAST_IP, ip), AuthMeColumns.NAME, AuthMeColumns.NICK_NAME)
                .stream()
                .map(values -> new NamedIdentifier(values.get(AuthMeColumns.NAME), values.get(AuthMeColumns.NICK_NAME)))
                .collect(Collectors.toList());
        } catch (SQLException e) {
            logSqlException(e);
            return Collections.emptyList();
        }
    }

    @Override
    public int countAuthsByEmail(String email) {
        return columnsHandler.count(eqIgnoreCase(AuthMeColumns.EMAIL, email));
    }

    @Override
    public boolean updateEmail(PlayerAuth auth) {
        return columnsHandler.update(auth, AuthMeColumns.EMAIL);
    }

    @Override
    public boolean isLogged(NamedIdentifier identifier) {
        try {
            DataSourceValue<Integer> result = columnsHandler.retrieve(identifier.getLowercaseName(), AuthMeColumns.IS_LOGGED);
            return result.rowExists() && Integer.valueOf(1).equals(result.getValue());
        } catch (SQLException e) {
            logSqlException(e);
            return false;
        }
    }

    @Override
    public void setLogged(NamedIdentifier identifier) {
        columnsHandler.update(identifier.getLowercaseName(), AuthMeColumns.IS_LOGGED, 1);
    }

    @Override
    public void setUnlogged(NamedIdentifier identifier) {
        columnsHandler.update(identifier.getLowercaseName(), AuthMeColumns.IS_LOGGED, 0);
    }

    @Override
    public void grantSession(NamedIdentifier identifier) {
        columnsHandler.update(identifier.getLowercaseName(), AuthMeColumns.HAS_SESSION, 1);
    }

    @Override
    public void revokeSession(NamedIdentifier identifier) {
        columnsHandler.update(identifier.getLowercaseName(), AuthMeColumns.HAS_SESSION, 0);
    }

    @Override
    public void purgeLogged() {
        columnsHandler.update(eq(AuthMeColumns.IS_LOGGED, 1), AuthMeColumns.IS_LOGGED, 0);
    }

    @Override
    public int getAccountsRegistered() {
        return columnsHandler.count(new AlwaysTruePredicate<>());
    }

    @Override
    public boolean updateRealName(NamedIdentifier identifier) {
        return columnsHandler.update(identifier.getLowercaseName(), AuthMeColumns.NICK_NAME, identifier.getRealName().orElse(null));
    }

    @Override
    public DataSourceValue<String> getEmail(NamedIdentifier identifier) {
        try {
            return columnsHandler.retrieve(identifier.getLowercaseName(), AuthMeColumns.EMAIL);
        } catch (SQLException e) {
            logSqlException(e);
            return DataSourceValueImpl.unknownRow();
        }
    }
}
