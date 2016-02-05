package fr.xephi.authme;

import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import fr.xephi.authme.settings.properties.PluginSettings;
import org.mcstats.Metrics;
import org.mcstats.Metrics.Graph;

import java.io.IOException;

public class MetricsStarter {

    private MetricsStarter() {
    }

    public static void setupMetrics(AuthMe plugin, NewSetting settings) {
        try {
            final Metrics metrics = new Metrics(plugin);

            final Graph languageGraph = metrics.createGraph("Messages Language");
            final String messagesLanguage = settings.getProperty(PluginSettings.MESSAGES_LANGUAGE);
            languageGraph.addPlotter(new Metrics.Plotter(messagesLanguage) {
                @Override
                public int getValue() {
                    return 1;
                }
            });

            final Graph databaseBackend = metrics.createGraph("Database Backend");
            final String dataSource = settings.getProperty(DatabaseSettings.BACKEND).toString();
            databaseBackend.addPlotter(new Metrics.Plotter(dataSource) {
                @Override
                public int getValue() {
                    return 1;
                }
            });

            // Submit metrics
            metrics.start();
        } catch (IOException e) {
          // Failed to submit the metrics data
          ConsoleLogger.logException("Can't start Metrics! The plugin will work anyway...", e);
        }
    }
}
