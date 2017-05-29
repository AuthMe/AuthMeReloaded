package fr.xephi.authme.settings;

import ch.jalu.configme.migration.PlainMigrationService;
import ch.jalu.configme.properties.Property;
import ch.jalu.configme.properties.StringListProperty;
import ch.jalu.configme.resource.PropertyResource;
import com.google.common.base.MoreObjects;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.output.LogLevel;
import fr.xephi.authme.process.register.RegisterSecondaryArgument;
import fr.xephi.authme.process.register.RegistrationType;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;

import javax.inject.Inject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static ch.jalu.configme.properties.PropertyInitializer.newListProperty;
import static ch.jalu.configme.properties.PropertyInitializer.newProperty;
import static fr.xephi.authme.settings.properties.RegistrationSettings.DELAY_JOIN_MESSAGE;
import static fr.xephi.authme.settings.properties.RegistrationSettings.REMOVE_JOIN_MESSAGE;
import static fr.xephi.authme.settings.properties.RegistrationSettings.REMOVE_LEAVE_MESSAGE;
import static fr.xephi.authme.settings.properties.RestrictionSettings.ALLOWED_NICKNAME_CHARACTERS;
import static fr.xephi.authme.settings.properties.RestrictionSettings.FORCE_SPAWN_LOCATION_AFTER_LOGIN;
import static fr.xephi.authme.settings.properties.RestrictionSettings.FORCE_SPAWN_ON_WORLDS;

/**
 * Service for verifying that the configuration is up-to-date.
 */
public class SettingsMigrationService extends PlainMigrationService {

    private final File pluginFolder;

    // Stores old commands that need to be migrated to the new commands configuration
    // We need to store it in here for retrieval when we build the CommandConfig. Retrieving it from the config.yml is
    // not possible since this migration service may trigger config.yml to be resaved. As the old command settings
    // don't exist in the code anymore, as soon as config.yml is resaved we lose this information.
    private List<String> onLoginCommands = Collections.emptyList();
    private List<String> onLoginConsoleCommands = Collections.emptyList();
    private List<String> onRegisterCommands = Collections.emptyList();
    private List<String> onRegisterConsoleCommands = Collections.emptyList();

    @Inject
    SettingsMigrationService(@DataFolder File pluginFolder) {
        this.pluginFolder = pluginFolder;
    }

    @Override
    @SuppressWarnings("checkstyle:BooleanExpressionComplexity")
    protected boolean performMigrations(PropertyResource resource, List<Property<?>> properties) {
        boolean changes = false;
        if ("[a-zA-Z0-9_?]*".equals(resource.getString(ALLOWED_NICKNAME_CHARACTERS.getPath()))) {
            resource.setValue(ALLOWED_NICKNAME_CHARACTERS.getPath(), "[a-zA-Z0-9_]*");
            changes = true;
        }

        gatherOldCommandSettings(resource);

        // Note ljacqu 20160211: Concatenating migration methods with | instead of the usual ||
        // ensures that all migrations will be performed
        return changes
            | performMailTextToFileMigration(resource)
            | migrateJoinLeaveMessages(resource)
            | migrateForceSpawnSettings(resource)
            | changeBooleanSettingToLogLevelProperty(resource)
            | hasOldHelpHeaderProperty(resource)
            | hasSupportOldPasswordProperty(resource)
            | convertToRegistrationType(resource)
            | mergeAndMovePermissionGroupSettings(resource)
            || hasDeprecatedProperties(resource);
    }

    private static boolean hasDeprecatedProperties(PropertyResource resource) {
        String[] deprecatedProperties = {
            "Converter.Rakamak.newPasswordHash", "Hooks.chestshop", "Hooks.legacyChestshop", "Hooks.notifications",
            "Passpartu", "Performances", "settings.restrictions.enablePasswordVerifier", "Xenoforo.predefinedSalt",
            "VeryGames", "settings.restrictions.allowAllCommandsIfRegistrationIsOptional", "DataSource.mySQLWebsite",
            "Hooks.customAttributes", "Security.stop.kickPlayersBeforeStopping",
            "settings.restrictions.keepCollisionsDisabled", "settings.forceCommands", "settings.forceCommandsAsConsole",
            "settings.forceRegisterCommands", "settings.forceRegisterCommandsAsConsole",
            "settings.sessions.sessionExpireOnIpChange"};
        for (String deprecatedPath : deprecatedProperties) {
            if (resource.contains(deprecatedPath)) {
                return true;
            }
        }
        return false;
    }

    // ----------------
    // Forced commands relocation (from config.yml to commands.yml)
    // ----------------
    private void gatherOldCommandSettings(PropertyResource resource) {
        onLoginCommands = getStringList(resource, "settings.forceCommands");
        onLoginConsoleCommands = getStringList(resource, "settings.forceCommandsAsConsole");
        onRegisterCommands = getStringList(resource, "settings.forceRegisterCommands");
        onRegisterConsoleCommands = getStringList(resource, "settings.forceRegisterCommandsAsConsole");
    }

    private List<String> getStringList(PropertyResource resource, String path) {
        return new StringListProperty(path).getValue(resource);
    }

    public List<String> getOnLoginCommands() {
        return onLoginCommands;
    }

    public List<String> getOnLoginConsoleCommands() {
        return onLoginConsoleCommands;
    }

    public List<String> getOnRegisterCommands() {
        return onRegisterCommands;
    }

    public List<String> getOnRegisterConsoleCommands() {
        return onRegisterConsoleCommands;
    }

    // --------
    // Specific migrations
    // --------

    /**
     * Check if {@code Email.mailText} is present and move it to the Email.html file if it doesn't exist yet.
     *
     * @param resource The property resource
     * @return True if a migration has been completed, false otherwise
     */
    private boolean performMailTextToFileMigration(PropertyResource resource) {
        final String oldSettingPath = "Email.mailText";
        final String oldMailText = resource.getString(oldSettingPath);
        if (oldMailText == null) {
            return false;
        }

        final File emailFile = new File(pluginFolder, "email.html");
        final String mailText = oldMailText
            .replace("<playername>", "<playername />").replace("%playername%", "<playername />")
            .replace("<servername>", "<servername />").replace("%servername%", "<servername />")
            .replace("<generatedpass>", "<generatedpass />").replace("%generatedpass%", "<generatedpass />")
            .replace("<image>", "<image />").replace("%image%", "<image />");
        if (!emailFile.exists()) {
            try (FileWriter fw = new FileWriter(emailFile)) {
                fw.write(mailText);
            } catch (IOException e) {
                ConsoleLogger.logException("Could not create email.html configuration file:", e);
            }
        }
        return true;
    }

    /**
     * Detect deprecated {@code settings.delayJoinLeaveMessages} and inform user of new "remove join messages"
     * and "remove leave messages" settings.
     *
     * @param resource The property resource
     * @return True if the configuration has changed, false otherwise
     */
    private static boolean migrateJoinLeaveMessages(PropertyResource resource) {
        Property<Boolean> oldDelayJoinProperty = newProperty("settings.delayJoinLeaveMessages", false);
        boolean hasMigrated = moveProperty(oldDelayJoinProperty, DELAY_JOIN_MESSAGE, resource);

        if (hasMigrated) {
            ConsoleLogger.info(String.format("Note that we now also have the settings %s and %s",
                REMOVE_JOIN_MESSAGE.getPath(), REMOVE_LEAVE_MESSAGE.getPath()));
        }
        return hasMigrated;
    }

    /**
     * Detects old "force spawn loc on join" and "force spawn on these worlds" settings and moves them
     * to the new paths.
     *
     * @param resource The property resource
     * @return True if the configuration has changed, false otherwise
     */
    private static boolean migrateForceSpawnSettings(PropertyResource resource) {
        Property<Boolean> oldForceLocEnabled = newProperty(
            "settings.restrictions.ForceSpawnLocOnJoinEnabled", false);
        Property<List<String>> oldForceWorlds = newListProperty(
            "settings.restrictions.ForceSpawnOnTheseWorlds", "world", "world_nether", "world_the_ed");

        return moveProperty(oldForceLocEnabled, FORCE_SPAWN_LOCATION_AFTER_LOGIN, resource)
            | moveProperty(oldForceWorlds, FORCE_SPAWN_ON_WORLDS, resource);
    }

    /**
     * Changes the old boolean property "hide spam from console" to the new property specifying
     * the log level.
     *
     * @param resource The property resource
     * @return True if the configuration has changed, false otherwise
     */
    private static boolean changeBooleanSettingToLogLevelProperty(PropertyResource resource) {
        final String oldPath = "Security.console.noConsoleSpam";
        final Property<LogLevel> newProperty = PluginSettings.LOG_LEVEL;
        if (!newProperty.isPresent(resource) && resource.contains(oldPath)) {
            ConsoleLogger.info("Moving '" + oldPath + "' to '" + newProperty.getPath() + "'");
            boolean oldValue = MoreObjects.firstNonNull(resource.getBoolean(oldPath), false);
            LogLevel level = oldValue ? LogLevel.INFO : LogLevel.FINE;
            resource.setValue(newProperty.getPath(), level.name());
            return true;
        }
        return false;
    }

    private static boolean hasOldHelpHeaderProperty(PropertyResource resource) {
        if (resource.contains("settings.helpHeader")) {
            ConsoleLogger.warning("Help header setting is now in messages/help_xx.yml, "
                + "please check the file to set it again");
            return true;
        }
        return false;
    }

    private static boolean hasSupportOldPasswordProperty(PropertyResource resource) {
        String path = "settings.security.supportOldPasswordHash";
        if (resource.contains(path)) {
            ConsoleLogger.warning("Property '" + path + "' is no longer supported. "
                + "Use '" + SecuritySettings.LEGACY_HASHES.getPath() + "' instead.");
            return true;
        }
        return false;
    }

    /**
     * Converts old boolean configurations for registration to the new enum properties, if applicable.
     *
     * @param resource The property resource
     * @return True if the configuration has changed, false otherwise
     */
    private static boolean convertToRegistrationType(PropertyResource resource) {
        String oldEmailRegisterPath = "settings.registration.enableEmailRegistrationSystem";
        if (RegistrationSettings.REGISTRATION_TYPE.isPresent(resource) || !resource.contains(oldEmailRegisterPath)) {
            return false;
        }

        boolean useEmail = newProperty(oldEmailRegisterPath, false).getValue(resource);
        RegistrationType registrationType = useEmail ? RegistrationType.EMAIL : RegistrationType.PASSWORD;

        String useConfirmationPath = useEmail
            ? "settings.registration.doubleEmailCheck"
            : "settings.restrictions.enablePasswordConfirmation";
        boolean hasConfirmation = newProperty(useConfirmationPath, false).getValue(resource);
        RegisterSecondaryArgument secondaryArgument = hasConfirmation
            ? RegisterSecondaryArgument.CONFIRMATION
            : RegisterSecondaryArgument.NONE;

        ConsoleLogger.warning("Merging old registration settings into '"
            + RegistrationSettings.REGISTRATION_TYPE.getPath() + "'");
        resource.setValue(RegistrationSettings.REGISTRATION_TYPE.getPath(), registrationType);
        resource.setValue(RegistrationSettings.REGISTER_SECOND_ARGUMENT.getPath(), secondaryArgument);
        return true;
    }

    /**
     * Migrates old permission group settings to the new configurations.
     *
     * @param resource The property resource
     * @return True if the configuration has changed, false otherwise
     */
    private static boolean mergeAndMovePermissionGroupSettings(PropertyResource resource) {
        boolean performedChanges;

        // We have two old settings replaced by only one: move the first non-empty one
        Property<String> oldUnloggedInGroup = newProperty("settings.security.unLoggedinGroup", "");
        Property<String> oldRegisteredGroup = newProperty("GroupOptions.RegisteredPlayerGroup", "");
        if (!oldUnloggedInGroup.getValue(resource).isEmpty()) {
            performedChanges = moveProperty(oldUnloggedInGroup, PluginSettings.REGISTERED_GROUP, resource);
        } else {
            performedChanges = moveProperty(oldRegisteredGroup, PluginSettings.REGISTERED_GROUP, resource);
        }

        // Move paths of other old options
        performedChanges |= moveProperty(newProperty("GroupOptions.UnregisteredPlayerGroup", ""),
            PluginSettings.UNREGISTERED_GROUP, resource);
        performedChanges |= moveProperty(newProperty("permission.EnablePermissionCheck", false),
            PluginSettings.ENABLE_PERMISSION_CHECK, resource);
        return performedChanges;
    }

    /**
     * Checks for an old property path and moves it to a new path if it is present and the new path is not yet set.
     *
     * @param oldProperty The old property (create a temporary {@link Property} object with the path)
     * @param newProperty The new property to move the value to
     * @param resource The property resource
     * @param <T> The type of the property
     * @return True if a migration has been done, false otherwise
     */
    private static <T> boolean moveProperty(Property<T> oldProperty,
                                            Property<T> newProperty,
                                            PropertyResource resource) {
        if (resource.contains(oldProperty.getPath())) {
            if (resource.contains(newProperty.getPath())) {
                ConsoleLogger.info("Detected deprecated property " + oldProperty.getPath());
            } else {
                ConsoleLogger.info("Renaming " + oldProperty.getPath() + " to " + newProperty.getPath());
                resource.setValue(newProperty.getPath(), oldProperty.getValue(resource));
            }
            return true;
        }
        return false;
    }

}
