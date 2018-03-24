package fr.xephi.authme.datasource;

import fr.xephi.authme.settings.Settings;

import java.util.HashMap;
import java.util.Map;

public class ColumnContext {

    private final Settings settings;
    private final Map<AuthMeColumns<?>, String> columnNames = new HashMap<>();

    public ColumnContext(Settings settings) {
        this.settings = settings;
    }

    public String getName(AuthMeColumns<?> column) {
        return columnNames.computeIfAbsent(column, k -> settings.getProperty(k.getNameProperty()));
    }
}
