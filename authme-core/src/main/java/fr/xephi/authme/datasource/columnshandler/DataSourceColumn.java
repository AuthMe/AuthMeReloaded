package fr.xephi.authme.datasource.columnshandler;

import ch.jalu.configme.properties.Property;
import ch.jalu.datasourcecolumns.Column;
import ch.jalu.datasourcecolumns.ColumnType;

/**
 * Basic {@link Column} implementation for AuthMe.
 *
 * @param <T> column type
 */
public class DataSourceColumn<T> implements Column<T, ColumnContext> {

    private final ColumnType<T> columnType;
    private final Property<String> nameProperty;
    private final boolean isOptional;
    private final boolean useDefaultForNull;

    /**
     * Constructor.
     *
     * @param type type of the column
     * @param nameProperty property defining the column name
     * @param isOptional whether or not the column can be skipped (if name is configured to empty string)
     * @param useDefaultForNull whether SQL DEFAULT should be used for null values (if supported by the database)
     */
    DataSourceColumn(ColumnType<T> type, Property<String> nameProperty, boolean isOptional, boolean useDefaultForNull) {
        this.columnType = type;
        this.nameProperty = nameProperty;
        this.isOptional = isOptional;
        this.useDefaultForNull = useDefaultForNull;
    }

    public Property<String> getNameProperty() {
        return nameProperty;
    }

    @Override
    public String resolveName(ColumnContext columnContext) {
        return columnContext.getName(this);
    }

    @Override
    public ColumnType<T> getType() {
        return columnType;
    }

    @Override
    public boolean isColumnUsed(ColumnContext columnContext) {
        return !isOptional || !resolveName(columnContext).isEmpty();
    }

    @Override
    public boolean useDefaultForNullValue(ColumnContext columnContext) {
        return useDefaultForNull && columnContext.hasDefaultSupport();
    }
}
