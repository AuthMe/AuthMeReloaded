package fr.xephi.authme.message;

/**
 * Keys for translatable messages managed by {@link Messages}.
 */
public enum MessageKey {

    /** In order to use this command you must be authenticated! */
    DENIED_COMMAND("error.denied_command"),

    /** A player with the same IP is already in game! */
    SAME_IP_ONLINE("on_join_validation.same_ip_online"),

    /** In order to chat you must be authenticated! */
    DENIED_CHAT("error.denied_chat"),

    /** AntiBot protection mode is enabled! You have to wait some minutes before joining the server. */
    KICK_ANTIBOT("antibot.kick_antibot"),

    /** This user isn't registered! */
    UNKNOWN_USER("error.unregistered_user"),

    /** You're not logged in! */
    NOT_LOGGED_IN("error.not_logged_in"),

    /** Usage: /login &lt;password&gt; */
    USAGE_LOGIN("login.command_usage"),

    /** Wrong password! */
    WRONG_PASSWORD("login.wrong_password"),

    /** Successfully unregistered! */
    UNREGISTERED_SUCCESS("unregister.success"),

    /** In-game registration is disabled! */
    REGISTRATION_DISABLED("registration.disabled"),

    /** Logged-in due to Session Reconnection. */
    SESSION_RECONNECTION("session.valid_session"),

    /** Successful login! */
    LOGIN_SUCCESS("login.success"),

    /** Your account isn't activated yet, please check your emails! */
    ACCOUNT_NOT_ACTIVATED("misc.account_not_activated"),

    /** You already have registered this username! */
    NAME_ALREADY_REGISTERED("registration.name_taken"),

    /** You don't have the permission to perform this action! */
    NO_PERMISSION("error.no_permission"),

    /** An unexpected error occurred, please contact an administrator! */
    ERROR("error.unexpected_error"),

    /** Please, login with the command: /login &lt;password&gt; */
    LOGIN_MESSAGE("login.login_request"),

    /** Please, register to the server with the command: /register &lt;password&gt; &lt;ConfirmPassword&gt; */
    REGISTER_MESSAGE("registration.register_request"),

    /** You have exceeded the maximum number of registrations (%reg_count/%max_acc %reg_names) for your connection! */
    MAX_REGISTER_EXCEEDED("error.max_registration", "%max_acc", "%reg_count", "%reg_names"),

    /** Usage: /register &lt;password&gt; &lt;ConfirmPassword&gt; */
    USAGE_REGISTER("registration.command_usage"),

    /** Usage: /unregister &lt;password&gt; */
    USAGE_UNREGISTER("unregister.command_usage"),

    /** Password changed successfully! */
    PASSWORD_CHANGED_SUCCESS("misc.password_changed"),

    /** Passwords didn't match, check them again! */
    PASSWORD_MATCH_ERROR("password.match_error"),

    /** You can't use your name as password, please choose another one... */
    PASSWORD_IS_USERNAME_ERROR("password.name_in_password"),

    /** The chosen password isn't safe, please choose another one... */
    PASSWORD_UNSAFE_ERROR("password.unsafe_password"),

    /** Your password contains illegal characters. Allowed chars: %valid_chars */
    PASSWORD_CHARACTERS_ERROR("password.forbidden_characters", "%valid_chars"),

    /** Your IP has been changed and your session data has expired! */
    SESSION_EXPIRED("session.invalid_session"),

    /** Only registered users can join the server! Please visit http://example.com to register yourself! */
    MUST_REGISTER_MESSAGE("registration.reg_only"),

    /** You're already logged in! */
    ALREADY_LOGGED_IN_ERROR("error.logged_in"),

    /** Logged out successfully! */
    LOGOUT_SUCCESS("misc.logout"),

    /** The same username is already playing on the server! */
    USERNAME_ALREADY_ONLINE_ERROR("on_join_validation.same_nick_online"),

    /** Successfully registered! */
    REGISTER_SUCCESS("registration.success"),

    /** Your password is too short or too long! Please try with another one! */
    INVALID_PASSWORD_LENGTH("password.wrong_length"),

    /** Configuration and database have been reloaded correctly! */
    CONFIG_RELOAD_SUCCESS("misc.reload"),

    /** Login timeout exceeded, you have been kicked from the server, please try again! */
    LOGIN_TIMEOUT_ERROR("login.timeout_error"),

    /** Usage: /changepassword &lt;oldPassword&gt; &lt;newPassword&gt; */
    USAGE_CHANGE_PASSWORD("misc.usage_change_password"),

    /** Your username is either too short or too long! */
    INVALID_NAME_LENGTH("on_join_validation.name_length"),

    /** Your username contains illegal characters. Allowed chars: %valid_chars */
    INVALID_NAME_CHARACTERS("on_join_validation.characters_in_name", "%valid_chars"),

    /** Please add your email to your account with the command: /email add &lt;yourEmail&gt; &lt;confirmEmail&gt; */
    ADD_EMAIL_MESSAGE("email.add_email_request"),

    /** Forgot your password? Please use the command: /email recovery &lt;yourEmail&gt; */
    FORGOT_PASSWORD_MESSAGE("recovery.forgot_password_hint"),

    /** To log in you have to solve a captcha code, please use the command: /captcha %captcha_code */
    USAGE_CAPTCHA("captcha.usage_captcha", "%captcha_code"),

    /** Wrong captcha, please type "/captcha %captcha_code" into the chat! */
    CAPTCHA_WRONG_ERROR("captcha.wrong_captcha", "%captcha_code"),

    /** Captcha code solved correctly! */
    CAPTCHA_SUCCESS("captcha.valid_captcha"),

    /** To register you have to solve a captcha first, please use the command: /captcha %captcha_code */
    CAPTCHA_FOR_REGISTRATION_REQUIRED("captcha.captcha_for_registration", "%captcha_code"),

    /** Valid captcha! You may now register with /register */
    REGISTER_CAPTCHA_SUCCESS("captcha.register_captcha_valid"),

    /** A VIP player has joined the server when it was full! */
    KICK_FOR_VIP("error.kick_for_vip"),

    /** The server is full, try again later! */
    KICK_FULL_SERVER("on_join_validation.kick_full_server"),

    /** Usage: /email add &lt;email&gt; &lt;confirmEmail&gt; */
    USAGE_ADD_EMAIL("email.usage_email_add"),

    /** Usage: /email change &lt;oldEmail&gt; &lt;newEmail&gt; */
    USAGE_CHANGE_EMAIL("email.usage_email_change"),

    /** Usage: /email recovery &lt;Email&gt; */
    USAGE_RECOVER_EMAIL("recovery.command_usage"),

    /** Invalid new email, try again! */
    INVALID_NEW_EMAIL("email.new_email_invalid"),

    /** Invalid old email, try again! */
    INVALID_OLD_EMAIL("email.old_email_invalid"),

    /** Invalid email address, try again! */
    INVALID_EMAIL("email.invalid"),

    /** Email address successfully added to your account! */
    EMAIL_ADDED_SUCCESS("email.added"),

    /** Adding email was not allowed */
    EMAIL_ADD_NOT_ALLOWED("email.add_not_allowed"),

    /** Please confirm your email address! */
    CONFIRM_EMAIL_MESSAGE("email.request_confirmation"),

    /** Email address changed correctly! */
    EMAIL_CHANGED_SUCCESS("email.changed"),

    /** Changing email was not allowed */
    EMAIL_CHANGE_NOT_ALLOWED("email.change_not_allowed"),

    /** Your current email address is: %email */
    EMAIL_SHOW("email.email_show", "%email"),

    /** You currently don't have email address associated with this account. */
    SHOW_NO_EMAIL("email.no_email_for_account"),

    /** Recovery email sent successfully! Please check your email inbox! */
    RECOVERY_EMAIL_SENT_MESSAGE("recovery.email_sent"),

    /** Your country is banned from this server! */
    COUNTRY_BANNED_ERROR("on_join_validation.country_banned"),

    /** [AntiBotService] AntiBot enabled due to the huge number of connections! */
    ANTIBOT_AUTO_ENABLED_MESSAGE("antibot.auto_enabled"),

    /** [AntiBotService] AntiBot disabled after %m minutes! */
    ANTIBOT_AUTO_DISABLED_MESSAGE("antibot.auto_disabled", "%m"),

    /** The email address is already being used */
    EMAIL_ALREADY_USED_ERROR("email.already_used"),

    /** Your secret code is %code. You can scan it from here %url */
    TWO_FACTOR_CREATE("two_factor.code_created", "%code", "%url"),

    /** Please confirm your code with /2fa confirm &lt;code&gt; */
    TWO_FACTOR_CREATE_CONFIRMATION_REQUIRED("two_factor.confirmation_required"),

    /** Please submit your two-factor authentication code with /2fa code &lt;code&gt; */
    TWO_FACTOR_CODE_REQUIRED("two_factor.code_required"),

    /** Two-factor authentication is already enabled for your account! */
    TWO_FACTOR_ALREADY_ENABLED("two_factor.already_enabled"),

    /** No 2fa key has been generated for you or it has expired. Please run /2fa add */
    TWO_FACTOR_ENABLE_ERROR_NO_CODE("two_factor.enable_error_no_code"),

    /** Successfully enabled two-factor authentication for your account */
    TWO_FACTOR_ENABLE_SUCCESS("two_factor.enable_success"),

    /** Wrong code or code has expired. Please run /2fa add */
    TWO_FACTOR_ENABLE_ERROR_WRONG_CODE("two_factor.enable_error_wrong_code"),

    /** Two-factor authentication is not enabled for your account. Run /2fa add */
    TWO_FACTOR_NOT_ENABLED_ERROR("two_factor.not_enabled_error"),

    /** Successfully removed two-factor auth from your account */
    TWO_FACTOR_REMOVED_SUCCESS("two_factor.removed_success"),

    /** Invalid code! */
    TWO_FACTOR_INVALID_CODE("two_factor.invalid_code"),

    /** You are not the owner of this account. Please choose another name! */
    NOT_OWNER_ERROR("on_join_validation.not_owner_error"),

    /** You should join using username %valid, not %invalid. */
    INVALID_NAME_CASE("on_join_validation.invalid_name_case", "%valid", "%invalid"),

    /** You have been temporarily banned for failing to log in too many times. */
    TEMPBAN_MAX_LOGINS("error.tempban_max_logins"),

    /** You own %count accounts: */
    ACCOUNTS_OWNED_SELF("misc.accounts_owned_self", "%count"),

    /** The player %name has %count accounts: */
    ACCOUNTS_OWNED_OTHER("misc.accounts_owned_other", "%name", "%count"),

    /** An admin just registered you; please log in again */
    KICK_FOR_ADMIN_REGISTER("registration.kicked_admin_registered"),

    /** Error: not all required settings are set for sending emails. Please contact an admin. */
    INCOMPLETE_EMAIL_SETTINGS("email.incomplete_settings"),

    /** The email could not be sent. Please contact an administrator. */
    EMAIL_SEND_FAILURE("email.send_failure"),

    /** A recovery code to reset your password has been sent to your email. */
    RECOVERY_CODE_SENT("recovery.code.code_sent"),

    /** The recovery code is not correct! You have %count tries remaining. */
    INCORRECT_RECOVERY_CODE("recovery.code.incorrect", "%count"),

    /**
     * You have exceeded the maximum number of attempts to enter the recovery code.
     * Use "/email recovery [email]" to generate a new one.
     */
    RECOVERY_TRIES_EXCEEDED("recovery.code.tries_exceeded"),

    /** Recovery code entered correctly! */
    RECOVERY_CODE_CORRECT("recovery.code.correct"),

    /** Please use the command /email setpassword to change your password immediately. */
    RECOVERY_CHANGE_PASSWORD("recovery.code.change_password"),

    /** You cannot change your password using this command anymore. */
    CHANGE_PASSWORD_EXPIRED("email.change_password_expired"),

    /** An email was already sent recently. You must wait %time before you can send a new one. */
    EMAIL_COOLDOWN_ERROR("email.email_cooldown_error", "%time"),

    /**
     * This command is sensitive and requires an email verification!
     * Check your inbox and follow the email's instructions.
     */
    VERIFICATION_CODE_REQUIRED("verification.code_required"),

    /** Usage: /verification &lt;code&gt; */
    USAGE_VERIFICATION_CODE("verification.command_usage"),

    /** Incorrect code, please type "/verification &lt;code&gt;" into the chat, using the code you received by email */
    INCORRECT_VERIFICATION_CODE("verification.incorrect_code"),

    /** Your identity has been verified! You can now execute all commands within the current session! */
    VERIFICATION_CODE_VERIFIED("verification.success"),

    /** You can already execute every sensitive command within the current session! */
    VERIFICATION_CODE_ALREADY_VERIFIED("verification.already_verified"),

    /** Your code has expired! Execute another sensitive command to get a new code! */
    VERIFICATION_CODE_EXPIRED("verification.code_expired"),

    /** To verify your identity you need to link an email address with your account! */
    VERIFICATION_CODE_EMAIL_NEEDED("verification.email_needed"),

    /** You used a command too fast! Please, join the server again and wait more before using any command. */
    QUICK_COMMAND_PROTECTION_KICK("on_join_validation.quick_command"),

    /** second */
    SECOND("time.second"),

    /** seconds */
    SECONDS("time.seconds"),

    /** minute */
    MINUTE("time.minute"),

    /** minutes */
    MINUTES("time.minutes"),

    /** hour */
    HOUR("time.hour"),

    /** hours */
    HOURS("time.hours"),

    /** day */
    DAY("time.day"),

    /** days */
    DAYS("time.days");


    private String key;
    private String[] tags;

    MessageKey(String key, String... tags) {
        this.key = key;
        this.tags = tags;
    }

    /**
     * Return the key used in the messages file.
     *
     * @return The key
     */
    public String getKey() {
        return key;
    }

    /**
     * Return a list of tags (texts) that are replaced with actual content in AuthMe.
     *
     * @return List of tags
     */
    public String[] getTags() {
        return tags;
    }

    @Override
    public String toString() {
        return key;
    }
}
