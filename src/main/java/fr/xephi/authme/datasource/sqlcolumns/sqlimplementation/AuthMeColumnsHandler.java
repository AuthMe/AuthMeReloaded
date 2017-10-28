package fr.xephi.authme.datasource.sqlcolumns.sqlimplementation;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.Columns;
import fr.xephi.authme.datasource.DataSourceResult;
import fr.xephi.authme.datasource.sqlcolumns.AuthMeColumns;
import fr.xephi.authme.datasource.sqlcolumns.Column;
import fr.xephi.authme.datasource.sqlcolumns.DataSourceValues;
import fr.xephi.authme.datasource.sqlcolumns.DependentColumn;
import fr.xephi.authme.datasource.sqlcolumns.UpdateValues;

import java.sql.Connection;
import java.sql.SQLException;

import static fr.xephi.authme.datasource.SqlDataSourceUtils.logSqlException;

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

    @Override
    public <T> DataSourceResult<T> retrieve(String name, Column<T, Columns> column) {
        try {
            return super.retrieve(name, column);
        } catch (SQLException e) {
            logSqlException(e);
            return DataSourceResult.unknownPlayer();
        }
    }

    @Override
    @SafeVarargs
    public final DataSourceValues retrieve(String name, Column<?, Columns>... columns) {
        try {
            return super.retrieve(name, columns);
        } catch (SQLException e) {
            logSqlException(e);
            return DataSourceValuesImpl.unknownPlayer();
        }
    }

    @Override
    public <T> boolean update(String name, Column<T, Columns> column, T value) {
        try {
            return super.update(name, column, value);
        } catch (SQLException e) {
            logSqlException(e);
            return false;
        }
    }

    @Override
    public boolean update(String name, UpdateValues<Columns> updateValues) {
        try {
            return super.update(name, updateValues);
        } catch (SQLException e) {
            logSqlException(e);
            return false;
        }
    }

    @SafeVarargs
    public final boolean update(PlayerAuth auth, DependentColumn<?, Columns, PlayerAuth>... columns) {
        try {
            return super.update(auth.getNickname(), auth, columns);
        } catch (SQLException e) {
            logSqlException(e);
            return false;
        }
    }

    @Override
    public boolean insert(UpdateValues<Columns> updateValues) {
        try {
            return super.insert(updateValues);
        } catch (SQLException e) {
            logSqlException(e);
            return false;
        }
    }

    @SafeVarargs
    public final boolean insert(PlayerAuth auth, DependentColumn<?, Columns, PlayerAuth>... columns) {
        try {
            return super.insert(auth, columns);
        } catch (SQLException e) {
            logSqlException(e);
            return false;
        }
    }
}
