package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.converter.Converter;
import fr.xephi.authme.converter.CrazyLoginConverter;
import fr.xephi.authme.converter.RakamakConverter;
import fr.xephi.authme.converter.RoyalAuthConverter;
import fr.xephi.authme.converter.SqliteToSql;
import fr.xephi.authme.converter.vAuthConverter;
import fr.xephi.authme.converter.xAuthConverter;
import fr.xephi.authme.initialization.AuthMeServiceInitializer;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.util.BukkitService;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.List;

/**
 * Converter command: launches conversion based on its parameters.
 */
public class ConverterCommand implements ExecutableCommand {

    @Inject
    private BukkitService bukkitService;

    @Inject
    private AuthMeServiceInitializer initializer;

    @Override
    public void executeCommand(final CommandSender sender, List<String> arguments, CommandService commandService) {
        // Get the conversion job
        String job = arguments.get(0);

        // Determine the job type
        ConvertType jobType = ConvertType.fromName(job);
        if (jobType == null) {
            commandService.send(sender, MessageKey.ERROR);
            return;
        }

        // Get the proper converter instance
        final Converter converter = initializer.newInstance(jobType.getConverterClass());

        // Run the convert job
        bukkitService.runTaskAsynchronously(new Runnable() {
            @Override
            public void run() {
                converter.execute(sender);
            }
        });

        // Show a status message
        sender.sendMessage("[AuthMe] Successfully converted from " + jobType.getName());
    }

    private enum ConvertType {
        XAUTH("xauth", xAuthConverter.class),
        CRAZYLOGIN("crazylogin", CrazyLoginConverter.class),
        RAKAMAK("rakamak", RakamakConverter.class),
        ROYALAUTH("royalauth", RoyalAuthConverter.class),
        VAUTH("vauth", vAuthConverter.class),
        SQLITETOSQL("sqlitetosql", SqliteToSql.class);

        private final String name;
        private final Class<? extends Converter> converterClass;

        ConvertType(String name, Class<? extends Converter> converterClass) {
            this.name = name;
            this.converterClass = converterClass;
        }

        public static ConvertType fromName(String name) {
            for (ConvertType type : ConvertType.values()) {
                if (type.getName().equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return null;
        }

        public String getName() {
            return this.name;
        }

        public Class<? extends Converter> getConverterClass() {
            return converterClass;
        }
    }
}
