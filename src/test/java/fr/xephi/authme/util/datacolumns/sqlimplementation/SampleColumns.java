package fr.xephi.authme.util.datacolumns.sqlimplementation;

import fr.xephi.authme.util.datacolumns.Column;
import fr.xephi.authme.util.datacolumns.ColumnType;
import fr.xephi.authme.util.datacolumns.StandardTypes;

public final class SampleColumns<T> implements Column<T, SampleContext> {

    public static final SampleColumns<Integer> ID =
        new SampleColumns<>(StandardTypes.INTEGER, false);

    public static final SampleColumns<String> NAME =
        new SampleColumns<>(StandardTypes.STRING, false);

    public static final SampleColumns<String> IP =
        new SampleColumns<>(StandardTypes.STRING, false);

    public static final SampleColumns<String> EMAIL =
        new SampleColumns<>(StandardTypes.STRING, true);

    public static final SampleColumns<Integer> IS_LOCKED =
        new SampleColumns<>(StandardTypes.INTEGER, true);

    public static final SampleColumns<Integer> IS_ACTIVE =
        new SampleColumns<>(StandardTypes.INTEGER, false);

    public static final SampleColumns<Long> LAST_LOGIN =
        new SampleColumns<>(StandardTypes.LONG, true);


    // -----------------------------------

    private final ColumnType<T> type;
    private final boolean isOptional;

    private SampleColumns(ColumnType<T> type, boolean isOptional) {
        this.type = type;
        this.isOptional = isOptional;
    }

    @Override
    public String resolveName(SampleContext context) {
        return context.resolveName(this);
    }

    @Override
    public ColumnType<T> getType() {
        return type;
    }

    @Override
    public boolean isColumnUsed(SampleContext context) {
        return !isOptional || resolveName(context).isEmpty();
    }
}
