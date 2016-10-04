package fr.xephi.authme.command.executable.authme;

import ch.jalu.injector.Injector;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.datasource.converter.Converter;
import fr.xephi.authme.datasource.converter.CrazyLoginConverter;
import fr.xephi.authme.datasource.converter.MySqlToSqlite;
import fr.xephi.authme.datasource.converter.RakamakConverter;
import fr.xephi.authme.datasource.converter.RoyalAuthConverter;
import fr.xephi.authme.datasource.converter.SqliteToSql;
import fr.xephi.authme.datasource.converter.vAuthConverter;
import fr.xephi.authme.datasource.converter.xAuthConverter;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

/**
 * Converter command: launches conversion based on its parameters.
 */
public class ConverterCommand implements ExecutableCommand {

    @VisibleForTesting
    static final Map<String, Class<? extends Converter>> CONVERTERS = getConverters();

    @Inject
    private CommandService commandService;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private Injector injector;

    @Override
    public void executeCommand(final CommandSender sender, List<String> arguments) {
        // Get the conversion job
        String job = arguments.get(0);

        // Determine the job type
        Class<? extends Converter> converterClass = CONVERTERS.get(job.toLowerCase());
        if (converterClass == null) {
            sender.sendMessage("[AuthMe] Converter does not exist!");
            return;
        }

        // Get the proper converter instance
        final Converter converter = injector.newInstance(converterClass);

        // Run the convert job
        bukkitService.runTaskAsynchronously(new Runnable() {
            @Override
            public void run() {
                try {
                    converter.execute(sender);
                } catch (Exception e) {
                    commandService.send(sender, MessageKey.ERROR);
                    ConsoleLogger.logException("Error during conversion:", e);
                }
            }
        });

        // Show a status message
        sender.sendMessage("[AuthMe] Successfully started " + job);
    }

    /**
     * Initializes a map with all available converters.
     *
     * @return map with all available converters
     */
    private static Map<String, Class<? extends Converter>> getConverters() {
        return ImmutableMap.<String, Class<? extends Converter>>builder()
            .put("xauth", xAuthConverter.class)
            .put("crazylogin", CrazyLoginConverter.class)
            .put("rakamak", RakamakConverter.class)
            .put("royalauth", RoyalAuthConverter.class)
            .put("vauth", vAuthConverter.class)
            .put("sqlitetosql", SqliteToSql.class)
            .put("mysqltosqlite", MySqlToSqlite.class)
            .build();
    }

}
