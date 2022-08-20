package fr.xephi.authme.command.executable.authme;

import ch.jalu.injector.factory.Factory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSortedMap;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.datasource.converter.Converter;
import fr.xephi.authme.datasource.converter.CrazyLoginConverter;
import fr.xephi.authme.datasource.converter.LoginSecurityConverter;
import fr.xephi.authme.datasource.converter.MySqlToSqlite;
import fr.xephi.authme.datasource.converter.RakamakConverter;
import fr.xephi.authme.datasource.converter.RoyalAuthConverter;
import fr.xephi.authme.datasource.converter.SqliteToSql;
import fr.xephi.authme.datasource.converter.VAuthConverter;
import fr.xephi.authme.datasource.converter.XAuthConverter;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Converter command: launches conversion based on its parameters.
 */
public class ConverterCommand implements ExecutableCommand {

    @VisibleForTesting
    static final Map<String, Class<? extends Converter>> CONVERTERS = getConverters();

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(ConverterCommand.class);

    @Inject
    private CommonService commonService;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private Factory<Converter> converterFactory;

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments) {
        Class<? extends Converter> converterClass = getConverterClassFromArgs(arguments);
        if (converterClass == null) {
            sender.sendMessage("Converters: " + String.join(", ", CONVERTERS.keySet()));
            return;
        }

        // Get the proper converter instance
        final Converter converter = converterFactory.newInstance(converterClass);

        // Run the convert job
        bukkitService.runTaskAsynchronously(() -> {
            try {
                converter.execute(sender);
            } catch (Exception e) {
                commonService.send(sender, MessageKey.ERROR);
                logger.logException("Error during conversion:", e);
            }
        });

        // Show a status message
        sender.sendMessage("[AuthMe] Successfully started " + arguments.get(0));
    }

    private static Class<? extends Converter> getConverterClassFromArgs(List<String> arguments) {
        return arguments.isEmpty()
            ? null
            : CONVERTERS.get(arguments.get(0).toLowerCase(Locale.ROOT));
    }

    /**
     * Initializes a map with all available converters.
     *
     * @return map with all available converters
     */
    private static Map<String, Class<? extends Converter>> getConverters() {
        return ImmutableSortedMap.<String, Class<? extends Converter>>naturalOrder()
            .put("xauth", XAuthConverter.class)
            .put("crazylogin", CrazyLoginConverter.class)
            .put("rakamak", RakamakConverter.class)
            .put("royalauth", RoyalAuthConverter.class)
            .put("vauth", VAuthConverter.class)
            .put("sqlitetosql", SqliteToSql.class)
            .put("mysqltosqlite", MySqlToSqlite.class)
            .put("loginsecurity", LoginSecurityConverter.class)
            .build();
    }
}
