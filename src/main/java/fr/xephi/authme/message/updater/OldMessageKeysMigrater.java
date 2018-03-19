package fr.xephi.authme.message.updater;

import ch.jalu.configme.resource.PropertyResource;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import fr.xephi.authme.message.MessageKey;

import java.util.Map;

import static com.google.common.collect.ImmutableMap.of;

/**
 * Migrates message files from the old keys (before 5.5) to the new ones.
 *
 * @see <a href="https://github.com/AuthMe/AuthMeReloaded/issues/1467">Issue #1467</a>
 */
final class OldMessageKeysMigrater {


    @VisibleForTesting
    static final Map<MessageKey, String> KEYS_TO_OLD_PATH = ImmutableMap.<MessageKey, String>builder()
        .put(MessageKey.LOGIN_SUCCESS, "login")
        .put(MessageKey.ERROR, "error")
        .put(MessageKey.DENIED_COMMAND, "denied_command")
        .put(MessageKey.SAME_IP_ONLINE, "same_ip_online")
        .put(MessageKey.DENIED_CHAT, "denied_chat")
        .put(MessageKey.KICK_ANTIBOT, "kick_antibot")
        .put(MessageKey.UNKNOWN_USER, "unknown_user")
        .put(MessageKey.NOT_LOGGED_IN, "not_logged_in")
        .put(MessageKey.USAGE_LOGIN, "usage_log")
        .put(MessageKey.WRONG_PASSWORD, "wrong_pwd")
        .put(MessageKey.UNREGISTERED_SUCCESS, "unregistered")
        .put(MessageKey.REGISTRATION_DISABLED, "reg_disabled")
        .put(MessageKey.SESSION_RECONNECTION, "valid_session")
        .put(MessageKey.ACCOUNT_NOT_ACTIVATED, "vb_nonActiv")
        .put(MessageKey.NAME_ALREADY_REGISTERED, "user_regged")
        .put(MessageKey.NO_PERMISSION, "no_perm")
        .put(MessageKey.LOGIN_MESSAGE, "login_msg")
        .put(MessageKey.REGISTER_MESSAGE, "reg_msg")
        .put(MessageKey.MAX_REGISTER_EXCEEDED, "max_reg")
        .put(MessageKey.USAGE_REGISTER, "usage_reg")
        .put(MessageKey.USAGE_UNREGISTER, "usage_unreg")
        .put(MessageKey.PASSWORD_CHANGED_SUCCESS, "pwd_changed")
        .put(MessageKey.PASSWORD_MATCH_ERROR, "password_error")
        .put(MessageKey.PASSWORD_IS_USERNAME_ERROR, "password_error_nick")
        .put(MessageKey.PASSWORD_UNSAFE_ERROR, "password_error_unsafe")
        .put(MessageKey.PASSWORD_CHARACTERS_ERROR, "password_error_chars")
        .put(MessageKey.SESSION_EXPIRED, "invalid_session")
        .put(MessageKey.MUST_REGISTER_MESSAGE, "reg_only")
        .put(MessageKey.ALREADY_LOGGED_IN_ERROR, "logged_in")
        .put(MessageKey.LOGOUT_SUCCESS, "logout")
        .put(MessageKey.USERNAME_ALREADY_ONLINE_ERROR, "same_nick")
        .put(MessageKey.REGISTER_SUCCESS, "registered")
        .put(MessageKey.INVALID_PASSWORD_LENGTH, "pass_len")
        .put(MessageKey.CONFIG_RELOAD_SUCCESS, "reload")
        .put(MessageKey.LOGIN_TIMEOUT_ERROR, "timeout")
        .put(MessageKey.USAGE_CHANGE_PASSWORD, "usage_changepassword")
        .put(MessageKey.INVALID_NAME_LENGTH, "name_len")
        .put(MessageKey.INVALID_NAME_CHARACTERS, "regex")
        .put(MessageKey.ADD_EMAIL_MESSAGE, "add_email")
        .put(MessageKey.FORGOT_PASSWORD_MESSAGE, "recovery_email")
        .put(MessageKey.USAGE_CAPTCHA, "usage_captcha")
        .put(MessageKey.CAPTCHA_WRONG_ERROR, "wrong_captcha")
        .put(MessageKey.CAPTCHA_SUCCESS, "valid_captcha")
        .put(MessageKey.CAPTCHA_FOR_REGISTRATION_REQUIRED, "captcha_for_registration")
        .put(MessageKey.REGISTER_CAPTCHA_SUCCESS, "register_captcha_valid")
        .put(MessageKey.KICK_FOR_VIP, "kick_forvip")
        .put(MessageKey.KICK_FULL_SERVER, "kick_fullserver")
        .put(MessageKey.USAGE_ADD_EMAIL, "usage_email_add")
        .put(MessageKey.USAGE_CHANGE_EMAIL, "usage_email_change")
        .put(MessageKey.USAGE_RECOVER_EMAIL, "usage_email_recovery")
        .put(MessageKey.INVALID_NEW_EMAIL, "new_email_invalid")
        .put(MessageKey.INVALID_OLD_EMAIL, "old_email_invalid")
        .put(MessageKey.INVALID_EMAIL, "email_invalid")
        .put(MessageKey.EMAIL_ADDED_SUCCESS, "email_added")
        .put(MessageKey.CONFIRM_EMAIL_MESSAGE, "email_confirm")
        .put(MessageKey.EMAIL_CHANGED_SUCCESS, "email_changed")
        .put(MessageKey.EMAIL_SHOW, "email_show")
        .put(MessageKey.SHOW_NO_EMAIL, "show_no_email")
        .put(MessageKey.RECOVERY_EMAIL_SENT_MESSAGE, "email_send")
        .put(MessageKey.COUNTRY_BANNED_ERROR, "country_banned")
        .put(MessageKey.ANTIBOT_AUTO_ENABLED_MESSAGE, "antibot_auto_enabled")
        .put(MessageKey.ANTIBOT_AUTO_DISABLED_MESSAGE, "antibot_auto_disabled")
        .put(MessageKey.EMAIL_ALREADY_USED_ERROR, "email_already_used")
        .put(MessageKey.TWO_FACTOR_CREATE, "two_factor_create")
        .put(MessageKey.NOT_OWNER_ERROR, "not_owner_error")
        .put(MessageKey.INVALID_NAME_CASE, "invalid_name_case")
        .put(MessageKey.TEMPBAN_MAX_LOGINS, "tempban_max_logins")
        .put(MessageKey.ACCOUNTS_OWNED_SELF, "accounts_owned_self")
        .put(MessageKey.ACCOUNTS_OWNED_OTHER, "accounts_owned_other")
        .put(MessageKey.KICK_FOR_ADMIN_REGISTER, "kicked_admin_registered")
        .put(MessageKey.INCOMPLETE_EMAIL_SETTINGS, "incomplete_email_settings")
        .put(MessageKey.EMAIL_SEND_FAILURE, "email_send_failure")
        .put(MessageKey.RECOVERY_CODE_SENT, "recovery_code_sent")
        .put(MessageKey.INCORRECT_RECOVERY_CODE, "recovery_code_incorrect")
        .put(MessageKey.RECOVERY_TRIES_EXCEEDED, "recovery_tries_exceeded")
        .put(MessageKey.RECOVERY_CODE_CORRECT, "recovery_code_correct")
        .put(MessageKey.RECOVERY_CHANGE_PASSWORD, "recovery_change_password")
        .put(MessageKey.CHANGE_PASSWORD_EXPIRED, "change_password_expired")
        .put(MessageKey.EMAIL_COOLDOWN_ERROR, "email_cooldown_error")
        .put(MessageKey.VERIFICATION_CODE_REQUIRED, "verification_code_required")
        .put(MessageKey.USAGE_VERIFICATION_CODE, "usage_verification_code")
        .put(MessageKey.INCORRECT_VERIFICATION_CODE, "incorrect_verification_code")
        .put(MessageKey.VERIFICATION_CODE_VERIFIED, "verification_code_verified")
        .put(MessageKey.VERIFICATION_CODE_ALREADY_VERIFIED, "verification_code_already_verified")
        .put(MessageKey.VERIFICATION_CODE_EXPIRED, "verification_code_expired")
        .put(MessageKey.VERIFICATION_CODE_EMAIL_NEEDED, "verification_code_email_needed")
        .put(MessageKey.SECOND, "second")
        .put(MessageKey.SECONDS, "seconds")
        .put(MessageKey.MINUTE, "minute")
        .put(MessageKey.MINUTES, "minutes")
        .put(MessageKey.HOUR, "hour")
        .put(MessageKey.HOURS, "hours")
        .put(MessageKey.DAY, "day")
        .put(MessageKey.DAYS, "days")
        .build();

    private static final Map<MessageKey, Map<String, String>> PLACEHOLDER_REPLACEMENTS =
        ImmutableMap.<MessageKey, Map<String, String>>builder()
            .put(MessageKey.PASSWORD_CHARACTERS_ERROR, of("REG_EX", "%valid_chars"))
            .put(MessageKey.INVALID_NAME_CHARACTERS, of("REG_EX", "%valid_chars"))
            .put(MessageKey.USAGE_CAPTCHA, of("<theCaptcha>", "%captcha_code"))
            .put(MessageKey.CAPTCHA_FOR_REGISTRATION_REQUIRED, of("<theCaptcha>", "%captcha_code"))
            .put(MessageKey.CAPTCHA_WRONG_ERROR, of("THE_CAPTCHA", "%captcha_code"))
            .build();

    private OldMessageKeysMigrater() {
    }

    /**
     * Migrates any existing old key paths to their new paths if no text has been defined for the new key.
     *
     * @param resource the resource to modify and read from
     * @return true if at least one message could be migrated, false otherwise
     */
    static boolean migrateOldPaths(PropertyResource resource) {
        boolean wasPropertyMoved = false;
        for (Map.Entry<MessageKey, String> migrationEntry : KEYS_TO_OLD_PATH.entrySet()) {
            wasPropertyMoved |= moveIfApplicable(resource, migrationEntry.getKey(), migrationEntry.getValue());
        }
        return wasPropertyMoved;
    }

    private static boolean moveIfApplicable(PropertyResource resource, MessageKey messageKey, String oldPath) {
        if (resource.getString(messageKey.getKey()) == null) {
            String textAtOldPath = resource.getString(oldPath);
            if (textAtOldPath != null) {
                textAtOldPath = replaceOldPlaceholders(messageKey, textAtOldPath);
                resource.setValue(messageKey.getKey(), textAtOldPath);
                return true;
            }
        }
        return false;
    }

    private static String replaceOldPlaceholders(MessageKey key, String text) {
        Map<String, String> replacements = PLACEHOLDER_REPLACEMENTS.get(key);
        if (replacements == null) {
            return text;
        }

        String newText = text;
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            newText = newText.replace(replacement.getKey(), replacement.getValue());
        }
        return newText;
    }
}
