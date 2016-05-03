package fr.xephi.authme.command;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.help.HelpProvider;
import fr.xephi.authme.hooks.PluginHooks;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.ValidationService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

/**
 * Service for implementations of {@link ExecutableCommand} to execute some common tasks.
 * This service basically wraps calls, forwarding them to other classes.
 */
public class CommandService {

    @Inject
    private AuthMe authMe;
    @Inject
    private Messages messages;
    @Inject
    private HelpProvider helpProvider;
    @Inject
    private CommandMapper commandMapper;
    @Inject
    private PasswordSecurity passwordSecurity;
    @Inject
    private PermissionsManager permissionsManager;
    @Inject
    private NewSetting settings;
    @Inject
    private PluginHooks pluginHooks;
    @Inject
    private SpawnLoader spawnLoader;
    @Inject
    private ValidationService validationService;
    @Inject
    private BukkitService bukkitService;

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
     * Return the AuthMe instance for further manipulation. Use only if other methods from
     * the command service cannot be used.
     *
     * @return The AuthMe instance
     */
    public AuthMe getAuthMe() {
        return authMe;
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

    /**
     * Return the settings manager.
     *
     * @return The settings manager
     */
    public NewSetting getSettings() {
        return settings;
    }

    public PluginHooks getPluginHooks() {
        return pluginHooks;
    }

    public SpawnLoader getSpawnLoader() {
        return spawnLoader;
    }

    /**
     * Verifies whether a password is valid according to the plugin settings.
     *
     * @param password the password to verify
     * @param username the username the password is associated with
     * @return message key with the password error, or {@code null} if password is valid
     */
    public MessageKey validatePassword(String password, String username) {
        return validationService.validatePassword(password, username);
    }

    public boolean validateEmail(String email) {
        return validationService.validateEmail(email);
    }

    public boolean isEmailFreeForRegistration(String email, CommandSender sender) {
        return validationService.isEmailFreeForRegistration(email, sender);
    }

    public Player getPlayer(String name) {
        return bukkitService.getPlayerExact(name);
    }

    public Collection<? extends Player> getOnlinePlayers() {
        return bukkitService.getOnlinePlayers();
    }

    public BukkitService getBukkitService() {
        return bukkitService;
    }

}
