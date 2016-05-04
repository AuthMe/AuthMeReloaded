package fr.xephi.authme.util;

import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.ProtectionSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.List;

/**
 * Validation service.
 */
public class ValidationService {

    private final NewSetting settings;
    private final DataSource dataSource;
    private final PermissionsManager permissionsManager;

    public ValidationService(NewSetting settings, DataSource dataSource, PermissionsManager permissionsManager) {
        this.settings = settings;
        this.dataSource = dataSource;
        this.permissionsManager = permissionsManager;
    }

    /**
     * Verifies whether a password is valid according to the plugin settings.
     *
     * @param password the password to verify
     * @param username the username the password is associated with
     * @return message key with the password error, or {@code null} if password is valid
     */
    public MessageKey validatePassword(String password, String username) {
        String passLow = password.toLowerCase();
        if (!passLow.matches(settings.getProperty(RestrictionSettings.ALLOWED_PASSWORD_REGEX))) {
            return MessageKey.PASSWORD_MATCH_ERROR;
        } else if (passLow.equalsIgnoreCase(username)) {
            return MessageKey.PASSWORD_IS_USERNAME_ERROR;
        } else if (password.length() < settings.getProperty(SecuritySettings.MIN_PASSWORD_LENGTH)
            || password.length() > settings.getProperty(SecuritySettings.MAX_PASSWORD_LENGTH)) {
            return MessageKey.INVALID_PASSWORD_LENGTH;
        } else if (settings.getProperty(SecuritySettings.UNSAFE_PASSWORDS).contains(passLow)) {
            // TODO #602 20160312: The UNSAFE_PASSWORDS should be all lowercase
            // -> introduce a lowercase String list property type
            return MessageKey.PASSWORD_UNSAFE_ERROR;
        }
        return null;
    }

    /**
     * Verifies whether the email is valid and admitted for use according to the plugin settings.
     *
     * @param email the email to verify
     * @return true if the email is valid, false otherwise
     */
    public boolean validateEmail(String email) {
        if (!email.contains("@") || "your@email.com".equalsIgnoreCase(email)) {
            return false;
        }
        final String emailDomain = email.split("@")[1];
        return validateWhitelistAndBlacklist(
            emailDomain, EmailSettings.DOMAIN_WHITELIST, EmailSettings.DOMAIN_BLACKLIST);
    }

    /**
     * Queries the database whether the email is still free for registration, i.e. whether the given
     * command sender may use the email to register a new account (as defined by settings and permissions).
     *
     * @param email the email to verify
     * @param sender the command sender
     * @return true if the email may be used, false otherwise (registration threshold has been exceeded)
     */
    public boolean isEmailFreeForRegistration(String email, CommandSender sender) {
        return permissionsManager.hasPermission(sender, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS)
            || dataSource.countAuthsByEmail(email) < settings.getProperty(EmailSettings.MAX_REG_PER_EMAIL);
    }

    /**
     * Checks whether the player's country is allowed to join the server, based on the given IP address
     * and the configured country whitelist or blacklist.
     *
     * @param hostAddress the IP address to verify
     * @return true if the IP address' country is allowed, false otherwise
     */
    public boolean isCountryAdmitted(String hostAddress) {
        // Check if we have restrictions on country, if not return true and avoid the country lookup
        if (settings.getProperty(ProtectionSettings.COUNTRIES_WHITELIST).isEmpty()
            && settings.getProperty(ProtectionSettings.COUNTRIES_BLACKLIST).isEmpty()) {
            return true;
        }

        String countryCode = GeoLiteAPI.getCountryCode(hostAddress);
        return validateWhitelistAndBlacklist(countryCode,
            ProtectionSettings.COUNTRIES_WHITELIST,
            ProtectionSettings.COUNTRIES_BLACKLIST);
    }

    /**
     * Verifies whether the given value is allowed according to the given whitelist and blacklist settings.
     * Whitelist has precedence over blacklist: if a whitelist is set, the value is rejected if not present
     * in the whitelist.
     *
     * @param value the value to verify
     * @param whitelist the whitelist property
     * @param blacklist the blacklist property
     * @return true if the value is admitted by the lists, false otherwise
     */
    private boolean validateWhitelistAndBlacklist(String value, Property<List<String>> whitelist,
                                                  Property<List<String>> blacklist) {
        List<String> whitelistValue = settings.getProperty(whitelist);
        if (!CollectionUtils.isEmpty(whitelistValue)) {
            return containsIgnoreCase(whitelistValue, value);
        }
        List<String> blacklistValue = settings.getProperty(blacklist);
        return CollectionUtils.isEmpty(blacklistValue) || !containsIgnoreCase(blacklistValue, value);
    }

    private static boolean containsIgnoreCase(Collection<String> coll, String needle) {
        for (String entry : coll) {
            if (entry.equalsIgnoreCase(needle)) {
                return true;
            }
        }
        return false;
    }
}
