package fr.xephi.authme.settings.custom;

import fr.xephi.authme.settings.domain.Comment;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.domain.SettingsClass;

import java.util.List;

import static fr.xephi.authme.settings.domain.Property.newProperty;
import static fr.xephi.authme.settings.domain.PropertyType.BOOLEAN;
import static fr.xephi.authme.settings.domain.PropertyType.INTEGER;
import static fr.xephi.authme.settings.domain.PropertyType.STRING;
import static fr.xephi.authme.settings.domain.PropertyType.STRING_LIST;

public class EmailSettings implements SettingsClass {

    @Comment("Email SMTP server host")
    public static final Property<String> SMTP_HOST =
        newProperty(STRING, "Email.mailSMTP", "smtp.gmail.com");

    @Comment("Email SMTP server port")
    public static final Property<Integer> SMTP_PORT =
        newProperty(INTEGER, "Email.mailPort", 465);

    @Comment("Email account which sends the mails")
    public static final Property<String> MAIL_ACCOUNT =
        newProperty(STRING, "Email.mailAccount", "");

    @Comment("Email account password")
    public static final Property<String> MAIL_PASSWORD =
        newProperty(STRING, "Email.mailPassword", "");

    @Comment("Custom sender name, replacing the mailAccount name in the email")
    public static final Property<String> MAIL_SENDER_NAME =
        newProperty("Email.mailSenderName", "");

    @Comment("Recovery password length")
    public static final Property<Integer> RECOVERY_PASSWORD_LENGTH =
        newProperty(INTEGER, "Email.RecoveryPasswordLength", 8);

    @Comment("Mail Subject")
    public static final Property<String> RECOVERY_MAIL_SUBJECT =
        newProperty(STRING, "Email.mailSubject", "Your new AuthMe password");

    @Comment("Like maxRegPerIP but with email")
    public static final Property<Integer> MAX_REG_PER_EMAIL =
        newProperty(INTEGER, "Email.maxRegPerEmail", 1);

    @Comment("Recall players to add an email?")
    public static final Property<Boolean> RECALL_PLAYERS =
        newProperty(BOOLEAN, "Email.recallPlayers", false);

    @Comment("Delay in minute for the recall scheduler")
    public static final Property<Integer> DELAY_RECALL =
        newProperty(INTEGER, "Email.delayRecall", 5);

    @Comment("Blacklist these domains for emails")
    public static final Property<List<String>> DOMAIN_BLACKLIST =
        newProperty(STRING_LIST, "Email.emailBlacklisted", "10minutemail.com");

    @Comment("Whitelist ONLY these domains for emails")
    public static final Property<List<String>> DOMAIN_WHITELIST =
        newProperty(STRING_LIST, "Email.emailWhitelisted");

    @Comment("Send the new password drawn in an image?")
    public static final Property<Boolean> PASSWORD_AS_IMAGE =
        newProperty(BOOLEAN, "Email.generateImage", false);

    @Comment("The OAuth2 token")
    public static final Property<String> OAUTH2_TOKEN =
        newProperty(STRING, "Email.emailOauth2Token", "");

    private EmailSettings() {
    }

}
