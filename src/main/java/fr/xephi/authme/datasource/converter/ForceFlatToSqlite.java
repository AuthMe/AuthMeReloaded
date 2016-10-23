package fr.xephi.authme.datasource.converter;

import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.FlatFile;

/**
 * Mandatory migration from the deprecated flat file datasource to SQLite.
 */
public class ForceFlatToSqlite extends AbstractDataSourceConverter<FlatFile> {

    private final FlatFile source;

    /**
     * Constructor.
     *
     * @param source The datasource to convert (flatfile)
     * @param destination The datasource to copy the data to (sqlite)
     */
    public ForceFlatToSqlite(FlatFile source, DataSource destination) {
        super(destination, destination.getType());
        this.source = source;
    }

    @Override
    public FlatFile getSource() {
        return source;
    }
}
