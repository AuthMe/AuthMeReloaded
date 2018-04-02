package fr.xephi.authme.datasource.columnshandler;

import ch.jalu.datasourcecolumns.sqlimplementation.PreparedStatementGenerator;
import ch.jalu.datasourcecolumns.sqlimplementation.PreparedStatementResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Implementation of {@link PreparedStatementGenerator} for MySQL which ensures that the connection
 * taken from the connection pool is also closed after the prepared statement has been executed.
 */
class MySqlPreparedStatementGenerator implements PreparedStatementGenerator {

    private final ConnectionSupplier connectionSupplier;

    MySqlPreparedStatementGenerator(ConnectionSupplier connectionSupplier) {
        this.connectionSupplier = connectionSupplier;
    }

    @Override
    public PreparedStatementResult create(String sql) throws SQLException {
        Connection connection = connectionSupplier.get();
        return new MySqlPreparedStatementResult(connection, connection.prepareStatement(sql));
    }

    /** Prepared statement result which also closes the associated connection. */
    private static final class MySqlPreparedStatementResult extends PreparedStatementResult {

        private final Connection connection;

        MySqlPreparedStatementResult(Connection connection, PreparedStatement preparedStatement) {
            super(preparedStatement);
            this.connection = connection;
        }

        @Override
        public void close() throws SQLException {
            super.close();
            connection.close();
        }
    }
}
