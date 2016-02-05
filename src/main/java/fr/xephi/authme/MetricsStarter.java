package fr.xephi.authme;

import java.io.IOException;

import org.mcstats.Metrics;
import org.mcstats.Metrics.Graph;

import fr.xephi.authme.settings.Settings;

public class MetricsStarter {

    public AuthMe plugin;

    public MetricsStarter(final AuthMe plugin) {
        this.plugin = plugin;
    }

    public void setupMetrics() {
        try {
            final Metrics metrics = new Metrics(plugin);

            final Graph messagesLanguage = metrics.createGraph("Messages Language");
            messagesLanguage.addPlotter(new Metrics.Plotter(Settings.messagesLanguage) {
                @Override
                public int getValue() {
                    return 1;
                }
            });

            final Graph databaseBackend = metrics.createGraph("Database Backend");
            databaseBackend.addPlotter(new Metrics.Plotter(Settings.getDataSource.toString()) {
                @Override
                public int getValue() {
                    return 1;
                }
            });

            // Submit metrics
            metrics.start();
        } catch (final IOException e) {
          // Failed to submit the metrics data
          ConsoleLogger.logException("Can't start Metrics! The plugin will work anyway...", e);
        }
    }
}
