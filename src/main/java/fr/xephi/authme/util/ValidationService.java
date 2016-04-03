package fr.xephi.authme.util;

import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.EmailSettings;
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

    public boolean validateEmail(String email) {
        if (!email.contains("@") || "your@email.com".equalsIgnoreCase(email)) {
            return false;
        }
        final String emailDomain = email.split("@")[1];

        List<String> whitelist = settings.getProperty(EmailSettings.DOMAIN_WHITELIST);
        if (!CollectionUtils.isEmpty(whitelist)) {
            return containsIgnoreCase(whitelist, emailDomain);
        }

        List<String> blacklist = settings.getProperty(EmailSettings.DOMAIN_BLACKLIST);
        return CollectionUtils.isEmpty(blacklist) || !containsIgnoreCase(blacklist, emailDomain);
    }

    public boolean isEmailFreeForRegistration(String email, CommandSender sender) {
        return permissionsManager.hasPermission(sender, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS)
            || dataSource.countAuthsByEmail(email) < settings.getProperty(EmailSettings.MAX_REG_PER_EMAIL);
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
