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

    public ProcessService(NewSetting settings, Messages messages, AuthMe authMe, DataSource dataSource,
                          IpAddressManager ipAddressManager, PasswordSecurity passwordSecurity, PluginHooks pluginHooks,
                          SpawnLoader spawnLoader) {
        this.settings = settings;
        this.messages = messages;
        this.authMe = authMe;
        this.dataSource = dataSource;
        this.ipAddressManager = ipAddressManager;
        this.passwordSecurity = passwordSecurity;
        this.pluginHooks = pluginHooks;
        this.spawnLoader = spawnLoader;
    }

    public <T> T getProperty(Property<T> property) {
        return settings.getProperty(property);
    }

    public NewSetting getSettings() {
        return settings;
    }

    public void send(CommandSender sender, MessageKey key) {
        messages.send(sender, key);
    }

    public void send(CommandSender sender, MessageKey key, String... replacements) {
        messages.send(sender, key, replacements);
    }

    public String[] retrieveMessage(MessageKey key) {
        return messages.retrieve(key);
    }

    public String retrieveSingleMessage(MessageKey key) {
        return messages.retrieveSingle(key);
    }

    public BukkitTask runTask(Runnable task) {
        return authMe.getServer().getScheduler().runTask(authMe, task);
    }

    public BukkitTask runTaskLater(Runnable task, long delay) {
        return authMe.getServer().getScheduler().runTaskLater(authMe, task, delay);
    }

    public int scheduleSyncDelayedTask(Runnable task) {
        return authMe.getServer().getScheduler().scheduleSyncDelayedTask(authMe, task);
    }

    public void callEvent(Event event) {
        authMe.getServer().getPluginManager().callEvent(event);
    }

    public AuthMe getAuthMe() {
        return authMe;
    }

    public IpAddressManager getIpAddressManager() {
        return ipAddressManager;
    }

    public HashedPassword computeHash(String password, String username) {
        return passwordSecurity.computeHash(password, username);
    }

    public PluginHooks getPluginHooks() {
        return pluginHooks;
    }

    public SpawnLoader getSpawnLoader() {
        return spawnLoader;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

}
