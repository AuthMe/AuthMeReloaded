package fr.xephi.authme.datasource.columnshandler;

import fr.xephi.authme.settings.Settings;

import java.util.HashMap;
import java.util.Map;

/**
 * Context for resolving the properties of {@link AuthMeColumns} entries.
 */
public class ColumnContext {

    private final Settings settings;
    private final Map<DataSourceColumn<?>, String> columnNames = new HashMap<>();
    private final boolean hasDefaultSupport;

    /**
     * Constructor.
     *
     * @param settings plugin settings
     * @param hasDefaultSupport whether or not the underlying database has support for the {@code DEFAULT} keyword
     */
    public ColumnContext(Settings settings, boolean hasDefaultSupport) {
        this.settings = settings;
        this.hasDefaultSupport = hasDefaultSupport;
    }

    public String getName(DataSourceColumn<?> column) {
        return columnNames.computeIfAbsent(column, k -> settings.getProperty(k.getNameProperty()));
    }

    public boolean hasDefaultSupport() {
        return hasDefaultSupport;
    }
}
