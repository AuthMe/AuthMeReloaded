package fr.xephi.authme.datasource.mysqlextensions;

import fr.xephi.authme.datasource.Columns;
import fr.xephi.authme.settings.Settings;

/**
 * Extension implementation that does not do anything.
 */
class NoOpExtension extends MySqlExtension {

    NoOpExtension(Settings settings, Columns col) {
        super(settings, col);
    }
}
