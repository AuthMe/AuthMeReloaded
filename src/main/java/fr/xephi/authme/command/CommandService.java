package fr.xephi.authme.command;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.help.HelpProvider;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.settings.custom.NewSetting;
import fr.xephi.authme.settings.domain.Property;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Service for implementations of {@link ExecutableCommand} to execute some common tasks.
 * This service basically wraps calls, forwarding them to other classes.
 */
public class CommandService {

    private final AuthMe authMe;
    private final Messages messages;
    private final HelpProvider helpProvider;
    private final CommandMapper commandMapper;
    private final PasswordSecurity passwordSecurity;
    private final PermissionsManager permissionsManager;
    private final NewSetting settings;

    /**
     * Constructor.
     *
     * @param authMe The plugin instance
     * @param commandMapper Command mapper
     * @param helpProvider Help provider
     * @param messages Messages instance
     * @param passwordSecurity The Password Security instance
     * @param permissionsManager The permissions manager
     * @param settings The settings manager
     */
    public CommandService(AuthMe authMe, CommandMapper commandMapper, HelpProvider helpProvider, Messages messages,
                          PasswordSecurity passwordSecurity, PermissionsManager permissionsManager,
                          NewSetting settings) {
        this.authMe = authMe;
        this.messages = messages;
        this.helpProvider = helpProvider;
        this.commandMapper = commandMapper;
        this.passwordSecurity = passwordSecurity;
        this.permissionsManager = permissionsManager;
        this.settings = settings;
    }

    /**
     * Send a message to a player.
     *
     * @param sender The command sender to send the message to
     * @param messageKey The message key to send
     */
    public void send(CommandSender sender, MessageKey messageKey) {
        messages.send(sender, messageKey);
    }

    /**
     * Send a message to a player.
     *
     * @param sender The command sender to send the message to
     * @param messageKey The message key to send
     * @param replacements The replacement arguments for the message key's tags
     */
    public void send(CommandSender sender, MessageKey messageKey, String... replacements) {
        messages.send(sender, messageKey, replacements);
    }

    /**
     * Map command parts to a command description.
     *
     * @param sender The command sender issuing the request (for permission check), or null to skip permissions
     * @param commandParts The received command parts to map to a command
     * @return The computed mapping result
     */
    public FoundCommandResult mapPartsToCommand(CommandSender sender, List<String> commandParts) {
        return commandMapper.mapPartsToCommand(sender, commandParts);
    }

    /**
     * Run the given task asynchronously with the Bukkit scheduler.
     *
     * @param task The task to run
     */
    public void runTaskAsynchronously(Runnable task) {
        authMe.getServer().getScheduler().runTaskAsynchronously(authMe, task);
    }

    /**
     * Return the AuthMe data source.
     *
     * @return The used data source
     */
    public DataSource getDataSource() {
        return authMe.getDataSource();
    }

    /**
     * Return the PasswordSecurity instance.
     *
     * @return The password security instance
     */
    public PasswordSecurity getPasswordSecurity() {
        return passwordSecurity;
    }

    /**
     * Output the help for a given command.
     *
     * @param sender The sender to output the help to
     * @param result The result to output information about
     * @param options Output options, see {@link HelpProvider}
     */
    public void outputHelp(CommandSender sender, FoundCommandResult result, int options) {
        List<String> lines = helpProvider.printHelp(sender, result, options);
        for (String line : lines) {
            sender.sendMessage(line);
        }
    }

    /**
     * Return the management instance of the plugin.
     *
     * @return The Management instance linked to the AuthMe instance
     */
    public Management getManagement() {
        return authMe.getManagement();
    }

    /**
     * Return the permissions manager.
     *
     * @return the permissions manager
     */
    public PermissionsManager getPermissionsManager() {
        return permissionsManager;
    }

    /**
     * Retrieve a message by its message key.
     *
     * @param key The message to retrieve
     * @return The message
     */
    public String[] retrieveMessage(MessageKey key) {
        return messages.retrieve(key);
    }

    /**
     * Retrieve the given property's value.
     *
     * @param property The property to retrieve
     * @param <T> The type of the property
     * @return The property's value
     */
    public <T> T getProperty(Property<T> property) {
        return settings.getProperty(property);
    }

}
