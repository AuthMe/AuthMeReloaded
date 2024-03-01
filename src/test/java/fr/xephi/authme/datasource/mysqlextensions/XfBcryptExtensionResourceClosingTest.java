package fr.xephi.authme.datasource.mysqlextensions;

import fr.xephi.authme.datasource.Columns;
import fr.xephi.authme.settings.Settings;

/**
 * Resource closing test for {@link XfBcryptExtension}.
 */
class XfBcryptExtensionResourceClosingTest extends AbstractMySqlExtensionResourceClosingTest {

    @Override
    protected MySqlExtension createExtension(Settings settings, Columns columns) {
        return new XfBcryptExtension(settings, columns);
    }
}
