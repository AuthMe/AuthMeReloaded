package fr.xephi.authme.datasource.columnshandler;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Supplier of connections to a database.
 */
@FunctionalInterface
public interface ConnectionSupplier {

    /**
     * Returns a connection to the database.
     *
     * @return the connection
     * @throws SQLException .
     */
    Connection get() throws SQLException;

}
