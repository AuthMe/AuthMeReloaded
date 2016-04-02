package fr.xephi.authme.process;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.IpAddressManager;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.hooks.PluginHooks;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.util.ValidationService;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.scheduler.BukkitTask;

/**
 * Service for asynchronous and synchronous processes.
 */
public class ProcessService {

    private final NewSetting settings;
    private final Messages messages;
    private final AuthMe authMe;
    private final DataSource dataSource;
    private final IpAddressManager ipAddressManager;
    private final PasswordSecurity passwordSecurity;
    private final PluginHooks pluginHooks;
    private final SpawnLoader spawnLoader;
    private final ValidationService validationService;

    public ProcessService(NewSetting settings, Messages messages, AuthMe authMe, DataSource dataSource,
                          IpAddressManager ipAddressManager, PasswordSecurity passwordSecurity, PluginHooks pluginHooks,
                          SpawnLoader spawnLoader, ValidationService validationService) {
        this.settings = settings;
        this.messages = messages;
        this.authMe = authMe;
        this.dataSource = dataSource;
        this.ipAddressManager = ipAddressManager;
        this.passwordSecurity = passwordSecurity;
        this.pluginHooks = pluginHooks;
        this.spawnLoader = spawnLoader;
        this.validationService = validationService;
    }

    /**
     * Retrieve a property's value.
     *
     * @param property the property to retrieve
     * @param <T> the property type
     * @return the property's value
     */
    public <T> T getProperty(Property<T> property) {
        return settings.getProperty(property);
    }

    /**
     * Return the settings manager.
     *
     * @return settings manager
     */
    public NewSetting getSettings() {
        return settings;
    }

    /**
     * Send a message to the command sender.
     *
     * @param sender the command sender
     * @param key the message key
     */
    public void send(CommandSender sender, MessageKey key) {
        messages.send(sender, key);
    }

    /**
     * Send a message to the command sender with the given replacements.
     *
     * @param sender the command sender
     * @param key the message key
     * @param replacements the replacements to apply to the message
     */
    public void send(CommandSender sender, MessageKey key, String... replacements) {
        messages.send(sender, key, replacements);
    }

    /**
     * Retrieve a message.
     *
     * @param key the key of the message
     * @return the message, split by line
     */
    public String[] retrieveMessage(MessageKey key) {
        return messages.retrieve(key);
    }

    /**
     * Retrieve a message as one piece.
     *
     * @param key the key of the message
     * @return the message
     */
    public String retrieveSingleMessage(MessageKey key) {
        return messages.retrieveSingle(key);
    }

    /**
     * Run a task.
     *
     * @param task the task to run
     * @return the assigned task id
     */
    public BukkitTask runTask(Runnable task) {
        return authMe.getServer().getScheduler().runTask(authMe, task);
    }

    /**
     * Run a task at a later time.
     *
     * @param task the task to run
     * @param delay the delay before running the task
     * @return the assigned task id
     */
    public BukkitTask runTaskLater(Runnable task, long delay) {
        return authMe.getServer().getScheduler().runTaskLater(authMe, task, delay);
    }

    /**
     * Schedule a synchronous delayed task.
     *
     * @param task the task to schedule
     * @return the task id
     */
    public int scheduleSyncDelayedTask(Runnable task) {
        return authMe.getServer().getScheduler().scheduleSyncDelayedTask(authMe, task);
    }

    /**
     * Emit an event.
     *
     * @param event the event to emit
     */
    public void callEvent(Event event) {
        authMe.getServer().getPluginManager().callEvent(event);
    }

    /**
     * Return the plugin instance.
     *
     * @return AuthMe instance
     */
    public AuthMe getAuthMe() {
        return authMe;
    }

    /**
     * Return the IP address manager.
     *
     * @return the ip address manager
     */
    public IpAddressManager getIpAddressManager() {
        return ipAddressManager;
    }

    /**
     * Compute the hash for the given password.
     *
     * @param password the password to hash
     * @param username the user to hash for
     * @return the resulting hash
     */
    public HashedPassword computeHash(String password, String username) {
        return passwordSecurity.computeHash(password, username);
    }

    /**
     * Return the PluginHooks manager.
     *
     * @return PluginHooks instance
     */
    public PluginHooks getPluginHooks() {
        return pluginHooks;
    }

    /**
     * Return the spawn manager.
     *
     * @return SpawnLoader instance
     */
    public SpawnLoader getSpawnLoader() {
        return spawnLoader;
    }

    /**
     * Return the plugin's datasource.
     *
     * @return the datasource
     */
    public DataSource getDataSource() {
        return dataSource;
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

}
