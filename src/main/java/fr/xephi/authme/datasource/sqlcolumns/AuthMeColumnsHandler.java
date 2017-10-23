package fr.xephi.authme.datasource.sqlcolumns;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.Columns;

import java.sql.Connection;

/**
 * Implementation for {@link AuthMeColumns}.
 */
public class AuthMeColumnsHandler extends SqlColumnsHandler<Columns, String> {

    /**
     * Constructor.
     *
     * @param connection connection to the database
     * @param context    the context object (for name resolution)
     * @param tableName  name of the SQL table
     */
    public AuthMeColumnsHandler(Connection connection, Columns context, String tableName, String nameColumn) {
        super(connection, context, tableName, nameColumn);
    }

    @SafeVarargs
    public final boolean update(PlayerAuth auth, DependentColumn<?, Columns, PlayerAuth>... columns) {
        return update(auth.getNickname(), auth, columns);
    }
}
