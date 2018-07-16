package fr.xephi.authme.datasource.columnshandler;

import ch.jalu.configme.properties.Property;
import ch.jalu.datasourcecolumns.ColumnType;
import ch.jalu.datasourcecolumns.DependentColumn;
import fr.xephi.authme.data.auth.PlayerAuth;

import java.util.function.Function;

/**
 * Implementation for columns which can also be retrieved from a {@link PlayerAuth} object.
 *
 * @param <T> column type
 */
public class PlayerAuthColumn<T> extends DataSourceColumn<T> implements DependentColumn<T, ColumnContext, PlayerAuth> {

    private final Function<PlayerAuth, T> playerAuthGetter;

    /*
     * Constructor. See parent class for details.
     */
    PlayerAuthColumn(ColumnType<T> type, Property<String> nameProperty, boolean isOptional, boolean useDefaultForNull,
                     Function<PlayerAuth, T> playerAuthGetter) {
        super(type, nameProperty, isOptional, useDefaultForNull);
        this.playerAuthGetter = playerAuthGetter;
    }

    @Override
    public T getValueFromDependent(PlayerAuth auth) {
        return playerAuthGetter.apply(auth);
    }
}
