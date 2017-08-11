package fr.xephi.authme.datasource.sqlextensions;

import fr.xephi.authme.datasource.Columns;
import fr.xephi.authme.settings.Settings;

import java.lang.reflect.Method;

/**
 * Resource closing test for {@link XfBcryptExtension}.
 */
public class XfBcryptExtensionResourceClosingTest extends AbstractSqlExtensionResourceClosingTest {

    public XfBcryptExtensionResourceClosingTest(Method method, String name) {
        super(method, name);
    }

    @Override
    protected SqlExtension createExtension(Settings settings, Columns columns) {
        return new XfBcryptExtension(settings, columns);
    }
}
