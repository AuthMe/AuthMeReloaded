package fr.xephi.authme.settings;

/**
 * Keys for translatable messages managed by {@link Messages}.
 */
public enum MessageKey {

    KICK_ANTIBOT("kick_antibot"),

    UNKNOWN_USER("unknown_user"),

    UNSAFE_QUIT_LOCATION("unsafe_spawn"),

    NOT_LOGGED_IN("not_logged_in"),

    REGISTER_VOLUNTARILY("reg_voluntarily"),

    USAGE_LOGIN("usage_log"),

    WRONG_PASSWORD("wrong_pwd"),

    UNREGISTERED_SUCCESS("unregistered"),

    REGISTRATION_DISABLED("reg_disabled"),

    SESSION_RECONNECTION("valid_session"),

    LOGIN_SUCCESS("login"),

    ACCOUNT_NOT_ACTIVATED("vb_nonActiv"),

    NAME_ALREADY_REGISTERED("user_regged"),

    NO_PERMISSION("no_perm"),

    ERROR("error"),

    LOGIN_MESSAGE("login_msg"),

    REGISTER_MESSAGE("reg_msg"),

    REGISTER_EMAIL_MESSAGE("reg_email_msg"),

    MAX_REGISTER_EXCEEDED("max_reg"),

    USAGE_REGISTER("usage_reg"),

    USAGE_UNREGISTER("usage_unreg"),

    PASSWORD_CHANGED_SUCCESS("pwd_changed"),

    USER_NOT_REGISTERED("user_unknown"),

    PASSWORD_MATCH_ERROR("password_error"),

    PASSWORD_IS_USERNAME_ERROR("password_error_nick"),

    PASSWORD_UNSAFE_ERROR("password_error_unsafe"),

    SESSION_EXPIRED("invalid_session"),

    MUST_REGISTER_MESSAGE("reg_only"),

    ALREADY_LOGGED_IN_ERROR("logged_in"),

    LOGOUT_SUCCESS("logout"),

    USERNAME_ALREADY_ONLINE_ERROR("same_nick"),

    REGISTER_SUCCESS("registered"),

    INVALID_PASSWORD_LENGTH("pass_len"),

    CONFIG_RELOAD_SUCCESS("reload"),

    LOGIN_TIMEOUT_ERROR("timeout"),

    USAGE_CHANGE_PASSWORD("usage_changepassword"),

    INVALID_NAME_LENGTH("name_len"),

    INVALID_NAME_CHARACTERS("regex"),

    ADD_EMAIL_MESSAGE("add_email"),

    FORGOT_PASSWORD_MESSAGE("recovery_email"),

    USAGE_CAPTCHA("usage_captcha"),

    CAPTCHA_WRONG_ERROR("wrong_captcha"),

    CAPTCHA_SUCCESS("valid_captcha"),

    KICK_FOR_VIP("kick_forvip"),

    KICK_FULL_SERVER("kick_fullserver"),

    USAGE_ADD_EMAIL("usage_email_add"),

    USAGE_RECOVER_EMAIL("usage_email_recovery"),

    INVALID_NEW_EMAIL("new_email_invalid"),

    INVALID_OLD_EMAIL("old_email_invalid"),

    INVALID_EMAIL("email_invalid"),

    EMAIL_ADDED_SUCCESS("email_added"),

    CONFIRM_EMAIL_MESSAGE("email_confirm"),

    EMAIL_CHANGED_SUCCESS("email_changed"),

    RECOVERY_EMAIL_SENT_MESSAGE("email_send"),

    RECOVERY_EMAIL_ALREADY_SENT_MESSAGE("email_exists"),

    COUNTRY_BANNED_ERROR("country_banned"),

    ANTIBOT_AUTO_ENABLED_MESSAGE("antibot_auto_enabled"),

    ANTIBOT_AUTO_DISABLED_MESSAGE("antibot_auto_disabled");


    private String key;

    MessageKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
