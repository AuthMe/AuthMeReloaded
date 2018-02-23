package fr.xephi.authme.settings.commandconfig;

import ch.jalu.configme.SettingsManager;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.GeoIpService;
import fr.xephi.authme.service.yaml.YamlFileResourceProvider;
import fr.xephi.authme.util.FileUtils;
import fr.xephi.authme.util.PlayerUtils;
import fr.xephi.authme.util.lazytags.Tag;
import fr.xephi.authme.util.lazytags.WrappedTagReplacer;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static fr.xephi.authme.util.lazytags.TagBuilder.createTag;

/**
 * Manages configurable commands to be run when various events occur.
 */
public class CommandManager implements Reloadable {

    private final File dataFolder;
    private final BukkitService bukkitService;
    private final GeoIpService geoIpService;
    private final CommandMigrationService commandMigrationService;
    private final List<Tag<Player>> availableTags = buildAvailableTags();

    private WrappedTagReplacer<Command, Player> onJoinCommands;
    private WrappedTagReplacer<OnLoginCommand, Player> onLoginCommands;
    private WrappedTagReplacer<Command, Player> onSessionLoginCommands;
    private WrappedTagReplacer<OnLoginCommand, Player> onFirstLoginCommands;
    private WrappedTagReplacer<Command, Player> onRegisterCommands;
    private WrappedTagReplacer<Command, Player> onUnregisterCommands;
    private WrappedTagReplacer<Command, Player> onLogoutCommands;

    @Inject
    CommandManager(@DataFolder File dataFolder, BukkitService bukkitService, GeoIpService geoIpService,
                   CommandMigrationService commandMigrationService) {
        this.dataFolder = dataFolder;
        this.bukkitService = bukkitService;
        this.geoIpService = geoIpService;
        this.commandMigrationService = commandMigrationService;
        reload();
    }

    /**
     * Runs the configured commands for when a player has joined.
     *
     * @param player the joining player
     */
    public void runCommandsOnJoin(Player player) {
        executeCommands(player, onJoinCommands.getAdaptedItems(player));
    }

    /**
     * Runs the configured commands for when a player has successfully registered.
     *
     * @param player the player who has registered
     */
    public void runCommandsOnRegister(Player player) {
        executeCommands(player, onRegisterCommands.getAdaptedItems(player));
    }

    /**
     * Runs the configured commands for when a player has logged in successfully.
     *
     * @param player the player that logged in
     * @param otherAccounts account names whose IP is the same as the player's
     */
    public void runCommandsOnLogin(Player player, List<String> otherAccounts) {
        final int numberOfOtherAccounts = otherAccounts.size();
        executeCommands(player, onLoginCommands.getAdaptedItems(player),
            cmd -> shouldCommandBeRun(cmd, numberOfOtherAccounts));
    }

    /**
     * Runs the configured commands for when a player has logged in successfully due to session.
     *
     * @param player the player that logged in
     */
    public void runCommandsOnSessionLogin(Player player) {
        executeCommands(player, onSessionLoginCommands.getAdaptedItems(player));
    }

    /**
     * Runs the configured commands for when a player logs in the first time.
     *
     * @param player the player that has logged in for the first time
     * @param otherAccounts account names whose IP is the same as the player's
     */
    public void runCommandsOnFirstLogin(Player player, List<String> otherAccounts) {
        final int numberOfOtherAccounts = otherAccounts.size();
        executeCommands(player, onFirstLoginCommands.getAdaptedItems(player),
            cmd -> shouldCommandBeRun(cmd, numberOfOtherAccounts));
    }

    /**
     * Runs the configured commands for when a player has been unregistered.
     *
     * @param player the player that has been unregistered
     */
    public void runCommandsOnUnregister(Player player) {
        executeCommands(player, onUnregisterCommands.getAdaptedItems(player));
    }

    /**
     * Runs the configured commands for when a player logs out (by command or by quitting the server).
     *
     * @param player the player that is no longer logged in
     */
    public void runCommandsOnLogout(Player player) {
        executeCommands(player, onLogoutCommands.getAdaptedItems(player));
    }

    private void executeCommands(Player player, List<Command> commands) {
        executeCommands(player, commands, c -> true);
    }

    private <T extends Command> void executeCommands(Player player, List<T> commands, Predicate<T> predicate) {
        for (T command : commands) {
            if (predicate.test(command)) {
                final String execution = command.getCommand();
                if (Executor.CONSOLE.equals(command.getExecutor())) {
                    bukkitService.dispatchConsoleCommand(execution);
                } else {
                    bukkitService.dispatchCommand(player, execution);
                }
            }
        }
    }

    private static boolean shouldCommandBeRun(OnLoginCommand command, int numberOfOtherAccounts) {
        return (!command.getIfNumberOfAccountsAtLeast().isPresent()
                || command.getIfNumberOfAccountsAtLeast().get() <= numberOfOtherAccounts)
            && (!command.getIfNumberOfAccountsLessThan().isPresent()
                || command.getIfNumberOfAccountsLessThan().get() > numberOfOtherAccounts);
    }

    @Override
    public void reload() {
        File file = new File(dataFolder, "commands.yml");
        FileUtils.copyFileFromResource(file, "commands.yml");

        SettingsManager settingsManager = new SettingsManager(
            YamlFileResourceProvider.loadFromFile(file), commandMigrationService, CommandSettingsHolder.class);
        CommandConfig commandConfig = settingsManager.getProperty(CommandSettingsHolder.COMMANDS);
        onJoinCommands = newReplacer(commandConfig.getOnJoin());
        onLoginCommands = newOnLoginCmdReplacer(commandConfig.getOnLogin());
        onFirstLoginCommands = newOnLoginCmdReplacer(commandConfig.getOnFirstLogin());
        onSessionLoginCommands = newReplacer(commandConfig.getOnSessionLogin());
        onRegisterCommands = newReplacer(commandConfig.getOnRegister());
        onUnregisterCommands = newReplacer(commandConfig.getOnUnregister());
        onLogoutCommands = newReplacer(commandConfig.getOnLogout());
    }

    private WrappedTagReplacer<Command, Player> newReplacer(Map<String, Command> commands) {
        return new WrappedTagReplacer<>(availableTags, commands.values(), Command::getCommand,
            (cmd, text) -> new Command(text, cmd.getExecutor()));
    }

    private WrappedTagReplacer<OnLoginCommand, Player> newOnLoginCmdReplacer(
        Map<String, OnLoginCommand> commands) {

        return new WrappedTagReplacer<>(availableTags, commands.values(), Command::getCommand,
            (cmd, text) -> new OnLoginCommand(text, cmd.getExecutor(), cmd.getIfNumberOfAccountsAtLeast(),
                cmd.getIfNumberOfAccountsLessThan()));
    }

    private List<Tag<Player>> buildAvailableTags() {
        return Arrays.asList(
            createTag("%p",       pl -> pl.getName()),
            createTag("%nick",    pl -> pl.getDisplayName()),
            createTag("%ip",      pl -> PlayerUtils.getPlayerIp(pl)),
            createTag("%country", pl -> geoIpService.getCountryName(PlayerUtils.getPlayerIp(pl))));
    }
}
