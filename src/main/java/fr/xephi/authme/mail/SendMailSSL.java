package fr.xephi.authme.mail;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.FileUtils;
import fr.xephi.authme.util.StringUtils;
import org.apache.commons.mail.EmailConstants;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;

import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.mail.Session;
import java.io.File;
import java.io.IOException;
import java.security.Security;
import java.util.Properties;

import static fr.xephi.authme.settings.properties.EmailSettings.MAIL_ACCOUNT;
import static fr.xephi.authme.settings.properties.EmailSettings.MAIL_PASSWORD;


/**
 * @author Xephi59
 */
public class SendMailSSL {

    @Inject
    private AuthMe plugin;
    @Inject
    private Settings settings;
    @Inject
    private BukkitService bukkitService;

    SendMailSSL() {
    }

    /**
     * Returns whether all necessary settings are set for sending mails.
     *
     * @return true if the necessary email settings are set, false otherwise
     */
    public boolean hasAllInformation() {
        return !settings.getProperty(MAIL_ACCOUNT).isEmpty()
            && !settings.getProperty(MAIL_PASSWORD).isEmpty();
    }

    /**
     * Sends an email to the user with his new password.
     *
     * @param name the name of the player
     * @param mailAddress the player's email
     * @param newPass the new password
     */
    public void sendPasswordMail(String name, String mailAddress, String newPass) {
        if (!hasAllInformation()) {
            ConsoleLogger.warning("Cannot perform email registration: not all email settings are complete");
            return;
        }

        final String mailText = replaceTagsForPasswordMail(settings.getPasswordEmailMessage(), name, newPass);
        bukkitService.runTaskAsynchronously(new Runnable() {

            @Override
            public void run() {
                HtmlEmail email;
                try {
                    email = initializeMail(mailAddress);
                } catch (EmailException e) {
                    ConsoleLogger.logException("Failed to create email with the given settings:", e);
                    return;
                }

                String content = mailText;
                // Generate an image?
                File file = null;
                if (settings.getProperty(EmailSettings.PASSWORD_AS_IMAGE)) {
                    try {
                        file = generateImage(name, plugin, newPass);
                        content = embedImageIntoEmailContent(file, email, content);
                    } catch (IOException | EmailException e) {
                        ConsoleLogger.logException(
                            "Unable to send new password as image for email " + mailAddress + ":", e);
                    }
                }

                sendEmail(content, email);
                FileUtils.delete(file);
            }
        });
    }

    public void sendRecoveryCode(String name, String email, String code) {
        String message = replaceTagsForRecoveryCodeMail(settings.getRecoveryCodeEmailMessage(),
            name, code, settings.getProperty(SecuritySettings.RECOVERY_CODE_HOURS_VALID));

        HtmlEmail htmlEmail;
        try {
            htmlEmail = initializeMail(email);
        } catch (EmailException e) {
            ConsoleLogger.logException("Failed to create email for recovery code:", e);
            return;
        }
        sendEmail(message, htmlEmail);
    }

    private static File generateImage(String name, AuthMe plugin, String newPass) throws IOException {
        ImageGenerator gen = new ImageGenerator(newPass);
        File file = new File(plugin.getDataFolder(), name + "_new_pass.jpg");
        ImageIO.write(gen.generateImage(), "jpg", file);
        return file;
    }

    private static String embedImageIntoEmailContent(File image, HtmlEmail email, String content)
            throws EmailException {
        DataSource source = new FileDataSource(image);
        String tag = email.embed(source, image.getName());
        return content.replace("<image />", "<img src=\"cid:" + tag + "\">");
    }

    private HtmlEmail initializeMail(String emailAddress) throws EmailException {
        String senderMail = settings.getProperty(EmailSettings.MAIL_ACCOUNT);
        String senderName = StringUtils.isEmpty(settings.getProperty(EmailSettings.MAIL_SENDER_NAME))
            ? senderMail
            : settings.getProperty(EmailSettings.MAIL_SENDER_NAME);
        String mailPassword = settings.getProperty(EmailSettings.MAIL_PASSWORD);
        int port = settings.getProperty(EmailSettings.SMTP_PORT);

        HtmlEmail email = new HtmlEmail();
        email.setCharset(EmailConstants.UTF_8);
        email.setSmtpPort(port);
        email.setHostName(settings.getProperty(EmailSettings.SMTP_HOST));
        email.addTo(emailAddress);
        email.setFrom(senderMail, senderName);
        email.setSubject(settings.getProperty(EmailSettings.RECOVERY_MAIL_SUBJECT));
        email.setAuthentication(senderMail, mailPassword);

        setPropertiesForPort(email, port);
        return email;
    }

    private static boolean sendEmail(String content, HtmlEmail email) {
        Thread.currentThread().setContextClassLoader(SendMailSSL.class.getClassLoader());
        try {
            email.setHtmlMsg(content);
            email.setTextMsg(content);
        } catch (EmailException e) {
            ConsoleLogger.logException("Your email.html config contains an error and cannot be sent:", e);
            return false;
        }
        try {
            email.send();
            return true;
        } catch (EmailException e) {
            ConsoleLogger.logException("Failed to send a mail to " + email.getToAddresses() + ":", e);
            return false;
        }
    }

    private String replaceTagsForPasswordMail(String mailText, String name, String newPass) {
        return mailText
            .replace("<playername />", name)
            .replace("<servername />", plugin.getServer().getServerName())
            .replace("<generatedpass />", newPass);
    }

    private String replaceTagsForRecoveryCodeMail(String mailText, String name, String code, int hoursValid) {
        return mailText
            .replace("<playername />", name)
            .replace("<servername />", plugin.getServer().getServerName())
            .replace("<recoverycode />", code)
            .replace("<hoursvalid />", String.valueOf(hoursValid));
    }

    private void setPropertiesForPort(HtmlEmail email, int port) throws EmailException {
        switch (port) {
            case 587:
                String oAuth2Token = settings.getProperty(EmailSettings.OAUTH2_TOKEN);
                if (!oAuth2Token.isEmpty()) {
                    if (Security.getProvider("Google OAuth2 Provider") == null) {
                        Security.addProvider(new OAuth2Provider());
                    }
                    Properties mailProperties = email.getMailSession().getProperties();
                    mailProperties.setProperty("mail.smtp.ssl.enable", "true");
                    mailProperties.setProperty("mail.smtp.auth.mechanisms", "XOAUTH2");
                    mailProperties.setProperty("mail.smtp.sasl.enable", "true");
                    mailProperties.setProperty("mail.smtp.sasl.mechanisms", "XOAUTH2");
                    mailProperties.setProperty("mail.smtp.auth.login.disable", "true");
                    mailProperties.setProperty("mail.smtp.auth.plain.disable", "true");
                    mailProperties.setProperty(OAuth2SaslClientFactory.OAUTH_TOKEN_PROP, oAuth2Token);
                    email.setMailSession(Session.getInstance(mailProperties));
                } else {
                    email.setStartTLSEnabled(true);
                    email.setStartTLSRequired(true);
                }
                break;
            case 25:
                email.setStartTLSEnabled(true);
                email.setSSLCheckServerIdentity(true);
                break;
            case 465:
                email.setSslSmtpPort(Integer.toString(port));
                email.setSSLOnConnect(true);
                break;
            default:
                email.setStartTLSEnabled(true);
                email.setSSLOnConnect(true);
                email.setSSLCheckServerIdentity(true);
        }
    }
}
