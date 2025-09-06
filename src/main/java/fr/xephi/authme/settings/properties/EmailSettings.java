package fr.xephi.authme.settings.properties;

import ch.jalu.configme.Comment;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.properties.Property;

import java.util.List;

import static ch.jalu.configme.properties.PropertyInitializer.newListProperty;
import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

public final class EmailSettings implements SettingsHolder {

    @Comment("Email SMTP server host")
    public static final Property<String> SMTP_HOST =
        newProperty("Email.mailSMTP", "smtp.gmail.com");

    @Comment("Email SMTP server port")
    public static final Property<Integer> SMTP_PORT =
        newProperty("Email.mailPort", 465);

    @Comment("Only affects port 25: enable TLS/STARTTLS?")
    public static final Property<Boolean> PORT25_USE_TLS =
        newProperty("Email.useTls", true);

    @Comment("Email account which sends the mails")
    public static final Property<String> MAIL_ACCOUNT =
        newProperty("Email.mailAccount", "");

    @Comment("Email account password")
    public static final Property<String> MAIL_PASSWORD =
        newProperty("Email.mailPassword", "");

    @Comment("Email address, fill when mailAccount is not the email address of the account")
    public static final Property<String> MAIL_ADDRESS =
        newProperty("Email.mailAddress", "");

    @Comment("Custom sender name, replacing the mailAccount name in the email")
    public static final Property<String> MAIL_SENDER_NAME =
        newProperty("Email.mailSenderName", "");

    @Comment("Recovery password length")
    public static final Property<Integer> RECOVERY_PASSWORD_LENGTH =
        newProperty("Email.RecoveryPasswordLength", 8);

    @Comment("Mail Subject")
    public static final Property<String> RECOVERY_MAIL_SUBJECT =
        newProperty("Email.mailSubject", "Your new AuthMe password");

    @Comment("Like maxRegPerIP but with email")
    public static final Property<Integer> MAX_REG_PER_EMAIL =
        newProperty("Email.maxRegPerEmail", 1);

    @Comment("Recall players to add an email?")
    public static final Property<Boolean> RECALL_PLAYERS =
        newProperty("Email.recallPlayers", false);

    @Comment("Delay in minute for the recall scheduler")
    public static final Property<Integer> DELAY_RECALL =
        newProperty("Email.delayRecall", 5);

    @Comment("Blacklist these domains for emails")
    public static final Property<List<String>> DOMAIN_BLACKLIST =
        newListProperty("Email.emailBlacklisted", "10minutemail.com");

    @Comment("Whitelist ONLY these domains for emails. Use 'r:' prefix for regex")
    public static final Property<List<String>> DOMAIN_WHITELIST =
        newListProperty("Email.emailWhitelisted");

    @Comment("Send the new password drawn in an image?")
    public static final Property<Boolean> PASSWORD_AS_IMAGE =
        newProperty("Email.generateImage", false);

    @Comment("The OAuth2 token")
    public static final Property<String> OAUTH2_TOKEN =
        newProperty("Email.emailOauth2Token", "");

    private EmailSettings() {
    }

}
