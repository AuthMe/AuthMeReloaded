package fr.xephi.authme.datasource.mysqlextensions;

import fr.xephi.authme.datasource.Columns;
import fr.xephi.authme.settings.Settings;

import java.lang.reflect.Method;

/**
 * Resource closing test for {@link WordpressExtension}.
 */
public class WordpressExtensionResourceClosingTest extends AbstractMySqlExtensionResourceClosingTest {

    public WordpressExtensionResourceClosingTest(Method method, String name) {
        super(method, name);
    }

    @Override
    protected MySqlExtension createExtension(Settings settings, Columns columns) {
        return new WordpressExtension(settings, columns);
    }
}
