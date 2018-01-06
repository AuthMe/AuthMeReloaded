package fr.xephi.authme.message;

/**
 * Keys for translatable messages managed by {@link Messages}.
 */
public enum MessageKey {

    /** In order to use this command you must be authenticated! */
    DENIED_COMMAND("denied_command"),

    /** A player with the same IP is already in game! */
    SAME_IP_ONLINE("same_ip_online"),

    /** In order to chat you must be authenticated! */
    DENIED_CHAT("denied_chat"),

    /** AntiBot protection mode is enabled! You have to wait some minutes before joining the server. */
    KICK_ANTIBOT("kick_antibot"),

    /** This user isn't registered! */
    UNKNOWN_USER("unknown_user"),

    /** You're not logged in! */
    NOT_LOGGED_IN("not_logged_in"),

    /** Usage: /login &lt;password&gt; */
    USAGE_LOGIN("usage_log"),

    /** Wrong password! */
    WRONG_PASSWORD("wrong_pwd"),

    /** Successfully unregistered! */
    UNREGISTERED_SUCCESS("unregistered"),

    /** In-game registration is disabled! */
    REGISTRATION_DISABLED("reg_disabled"),

    /** Logged-in due to Session Reconnection. */
    SESSION_RECONNECTION("valid_session"),

    /** Successful login! */
    LOGIN_SUCCESS("login"),

    /** Your account isn't activated yet, please check your emails! */
    ACCOUNT_NOT_ACTIVATED("vb_nonActiv"),

    /** You already have registered this username! */
    NAME_ALREADY_REGISTERED("user_regged"),

    /** You don't have the permission to perform this action! */
    NO_PERMISSION("no_perm"),

    /** An unexpected error occurred, please contact an administrator! */
    ERROR("error"),

    /** Please, login with the command: /login &lt;password&gt; */
    LOGIN_MESSAGE("login_msg"),

    /** Please, register to the server with the command: /register &lt;password&gt; &lt;ConfirmPassword&gt; */
    REGISTER_MESSAGE("reg_msg"),

    /** You have exceeded the maximum number of registrations (%reg_count/%max_acc %reg_names) for your connection! */
    MAX_REGISTER_EXCEEDED("max_reg", "%max_acc", "%reg_count", "%reg_names"),

    /** Usage: /register &lt;password&gt; &lt;ConfirmPassword&gt; */
    USAGE_REGISTER("usage_reg"),

    /** Usage: /unregister &lt;password&gt; */
    USAGE_UNREGISTER("usage_unreg"),

    /** Password changed successfully! */
    PASSWORD_CHANGED_SUCCESS("pwd_changed"),

    /** Passwords didn't match, check them again! */
    PASSWORD_MATCH_ERROR("password_error"),

    /** You can't use your name as password, please choose another one... */
    PASSWORD_IS_USERNAME_ERROR("password_error_nick"),

    /** The chosen password isn't safe, please choose another one... */
    PASSWORD_UNSAFE_ERROR("password_error_unsafe"),

    /** Your password contains illegal characters. Allowed chars: REG_EX */
    PASSWORD_CHARACTERS_ERROR("password_error_chars", "REG_EX"),

    /** Your IP has been changed and your session data has expired! */
    SESSION_EXPIRED("invalid_session"),

    /** Only registered users can join the server! Please visit http://example.com to register yourself! */
    MUST_REGISTER_MESSAGE("reg_only"),

    /** You're already logged in! */
    ALREADY_LOGGED_IN_ERROR("logged_in"),

    /** Logged out successfully! */
    LOGOUT_SUCCESS("logout"),

    /** The same username is already playing on the server! */
    USERNAME_ALREADY_ONLINE_ERROR("same_nick"),

    /** Successfully registered! */
    REGISTER_SUCCESS("registered"),

    /** Your password is too short or too long! Please try with another one! */
    INVALID_PASSWORD_LENGTH("pass_len"),

    /** Configuration and database have been reloaded correctly! */
    CONFIG_RELOAD_SUCCESS("reload"),

    /** Login timeout exceeded, you have been kicked from the server, please try again! */
    LOGIN_TIMEOUT_ERROR("timeout"),

    /** Usage: /changepassword &lt;oldPassword&gt; &lt;newPassword&gt; */
    USAGE_CHANGE_PASSWORD("usage_changepassword"),

    /** Your username is either too short or too long! */
    INVALID_NAME_LENGTH("name_len"),

    /** Your username contains illegal characters. Allowed chars: REG_EX */
    INVALID_NAME_CHARACTERS("regex", "REG_EX"),

    /** Please add your email to your account with the command: /email add &lt;yourEmail&gt; &lt;confirmEmail&gt; */
    ADD_EMAIL_MESSAGE("add_email"),

    /** Forgot your password? Please use the command: /email recovery &lt;yourEmail&gt; */
    FORGOT_PASSWORD_MESSAGE("recovery_email"),

    /** To login you have to solve a captcha code, please use the command: /captcha &lt;theCaptcha&gt; */
    USAGE_CAPTCHA("usage_captcha", "<theCaptcha>"),

    /** Wrong captcha, please type "/captcha THE_CAPTCHA" into the chat! */
    CAPTCHA_WRONG_ERROR("wrong_captcha", "THE_CAPTCHA"),

    /** Captcha code solved correctly! */
    CAPTCHA_SUCCESS("valid_captcha"),

    /** To register you have to solve a captcha code first, please use the command: /captcha &lt;theCaptcha&gt; */
    CAPTCHA_FOR_REGISTRATION_REQUIRED("captcha_for_registration", "<theCaptcha>"),

    /** Valid captcha! You may now register with /register */
    REGISTER_CAPTCHA_SUCCESS("register_captcha_valid"),

    /** A VIP player has joined the server when it was full! */
    KICK_FOR_VIP("kick_forvip"),

    /** The server is full, try again later! */
    KICK_FULL_SERVER("kick_fullserver"),

    /** Usage: /email add &lt;email&gt; &lt;confirmEmail&gt; */
    USAGE_ADD_EMAIL("usage_email_add"),

    /** Usage: /email change &lt;oldEmail&gt; &lt;newEmail&gt; */
    USAGE_CHANGE_EMAIL("usage_email_change"),

    /** Usage: /email recovery &lt;Email&gt; */
    USAGE_RECOVER_EMAIL("usage_email_recovery"),

    /** Invalid new email, try again! */
    INVALID_NEW_EMAIL("new_email_invalid"),

    /** Invalid old email, try again! */
    INVALID_OLD_EMAIL("old_email_invalid"),

    /** Invalid email address, try again! */
    INVALID_EMAIL("email_invalid"),

    /** Email address successfully added to your account! */
    EMAIL_ADDED_SUCCESS("email_added"),

    /** Please confirm your email address! */
    CONFIRM_EMAIL_MESSAGE("email_confirm"),

    /** Email address changed correctly! */
    EMAIL_CHANGED_SUCCESS("email_changed"),

    /** Your current email address is: %email */
    EMAIL_SHOW("email_show", "%email"),

    /** You currently don't have email address associated with this account. */
    SHOW_NO_EMAIL("show_no_email"),

    /** Recovery email sent successfully! Please check your email inbox! */
    RECOVERY_EMAIL_SENT_MESSAGE("email_send"),

    /** Your country is banned from this server! */
    COUNTRY_BANNED_ERROR("country_banned"),

    /** [AntiBotService] AntiBot enabled due to the huge number of connections! */
    ANTIBOT_AUTO_ENABLED_MESSAGE("antibot_auto_enabled"),

    /** [AntiBotService] AntiBot disabled after %m minutes! */
    ANTIBOT_AUTO_DISABLED_MESSAGE("antibot_auto_disabled", "%m"),

    /** The email address is already being used */
    EMAIL_ALREADY_USED_ERROR("email_already_used"),

    /** Your secret code is %code. You can scan it from here %url */
    TWO_FACTOR_CREATE("two_factor_create", "%code", "%url"),

    /** You are not the owner of this account. Please choose another name! */
    NOT_OWNER_ERROR("not_owner_error"),

    /** You should join using username %valid, not %invalid. */
    INVALID_NAME_CASE("invalid_name_case", "%valid", "%invalid"),

    /** You have been temporarily banned for failing to log in too many times. */
    TEMPBAN_MAX_LOGINS("tempban_max_logins"),

    /** You own %count accounts: */
    ACCOUNTS_OWNED_SELF("accounts_owned_self", "%count"),

    /** The player %name has %count accounts: */
    ACCOUNTS_OWNED_OTHER("accounts_owned_other", "%name", "%count"),

    /** An admin just registered you; please log in again */
    KICK_FOR_ADMIN_REGISTER("kicked_admin_registered"),

    /** Error: not all required settings are set for sending emails. Please contact an admin. */
    INCOMPLETE_EMAIL_SETTINGS("incomplete_email_settings"),

    /** The email could not be sent. Please contact an administrator. */
    EMAIL_SEND_FAILURE("email_send_failure"),

    /** A recovery code to reset your password has been sent to your email. */
    RECOVERY_CODE_SENT("recovery_code_sent"),

    /** The recovery code is not correct! You have %count tries remaining. */
    INCORRECT_RECOVERY_CODE("recovery_code_incorrect", "%count"),

    /**
     * You have exceeded the maximum number of attempts to enter the recovery code.
     * Use "/email recovery [email]" to generate a new one.
     */
    RECOVERY_TRIES_EXCEEDED("recovery_tries_exceeded"),

    /** Recovery code entered correctly! */
    RECOVERY_CODE_CORRECT("recovery_code_correct"),

    /** Please use the command /email setpassword to change your password immediately. */
    RECOVERY_CHANGE_PASSWORD("recovery_change_password"),

    /** You cannot change your password using this command anymore. */
    CHANGE_PASSWORD_EXPIRED("change_password_expired"),

    /** An email was already sent recently. You must wait %time before you can send a new one. */
    EMAIL_COOLDOWN_ERROR("email_cooldown_error", "%time"),

    /**
     * The command you are trying to execute is sensitive and requires a verification!
     * A verification code has been sent to your email,
     * run the command "/verification [code]" to verify your identity.
     */
    VERIFICATION_CODE_REQUIRED("verification_code_required"),

    /** Usage: /verification &lt;code&gt; */
    USAGE_VERIFICATION_CODE("usage_verification_code"),

    /** Incorrect code, please type "/verification &lt;code&gt;" into the chat! */
    INCORRECT_VERIFICATION_CODE("incorrect_verification_code"),

    /**
     * Your identity has been verified!
     * You can now execute every sensitive command within the current session!
     */
    VERIFICATION_CODE_VERIFIED("verification_code_verified"),

    /**
     * You can already execute every sensitive command within the current session!
     */
    VERIFICATION_CODE_ALREADY_VERIFIED("verification_code_already_verified"),

    /** Your code has expired! Execute another sensitive command to get a new code! */
    VERIFICATION_CODE_EXPIRED("verification_code_expired"),

    /** To verify your identity you need to link an email address with your account! */
    VERIFICATION_CODE_EMAIL_NEEDED("verification_code_email_needed"),

    /** second */
    SECOND("second"),

    /** seconds */
    SECONDS("seconds"),

    /** minute */
    MINUTE("minute"),

    /** minutes */
    MINUTES("minutes"),

    /** hour */
    HOUR("hour"),

    /** hours */
    HOURS("hours"),

    /** day */
    DAY("day"),

    /** days */
    DAYS("days");


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
