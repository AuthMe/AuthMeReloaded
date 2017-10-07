package fr.xephi.authme.datasource.converter;

import fr.xephi.authme.data.auth.PlayerAuth;
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

    @Override
    protected void adaptPlayerAuth(PlayerAuth auth) {
        // Issue #1120: FlatFile returns PlayerAuth objects with realname = lower-case name all the time.
        // We don't want to take this over into the new data source.
        auth.setRealName(null);

        // Note ljacqu 20171008: Check old defaults and remove here; we don't migrate flatfile data within
        // the flat file medium (see #814 and #792).
        if ("your@email.com".equals(auth.getEmail())) {
            auth.setEmail(null);
        }
        if ("127.0.0.1".equals(auth.getLastIp())) {
            auth.setLastIp(null);
        }
    }
}
