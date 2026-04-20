package fr.xephi.authme.datasource.mysqlextensions;

import fr.xephi.authme.datasource.Columns;
import fr.xephi.authme.settings.Settings;

import java.lang.reflect.Method;

/**
 * Resource closing test for {@link XfBcryptExtension}.
 */
public class XfBcryptExtensionResourceClosingTest extends AbstractMySqlExtensionResourceClosingTest {

    public XfBcryptExtensionResourceClosingTest(Method method, String name) {
        super(method, name);
    }

    @Override
    protected MySqlExtension createExtension(Settings settings, Columns columns) {
        return new XfBcryptExtension(settings, columns);
    }
}
