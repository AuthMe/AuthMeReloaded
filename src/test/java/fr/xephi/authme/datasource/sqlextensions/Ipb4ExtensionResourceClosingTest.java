package fr.xephi.authme.datasource.sqlextensions;

import fr.xephi.authme.datasource.Columns;
import fr.xephi.authme.settings.Settings;

import java.lang.reflect.Method;

/**
 * Resource closing test for {@link Ipb4Extension}.
 */
public class Ipb4ExtensionResourceClosingTest extends AbstractSqlExtensionResourceClosingTest {

    public Ipb4ExtensionResourceClosingTest(Method method, String name) {
        super(method, name);
    }

    @Override
    protected SqlExtension createExtension(Settings settings, Columns columns) {
        return new Ipb4Extension(settings, columns);
    }
}
