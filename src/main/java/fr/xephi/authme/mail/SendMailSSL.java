package fr.xephi.authme.mail;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.util.StringUtils;
import org.apache.commons.mail.EmailConstants;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.bukkit.Bukkit;

import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.imageio.ImageIO;
import javax.mail.Session;
import java.io.File;
import java.io.IOException;
import java.security.Security;
import java.util.Properties;


/**
 * @author Xephi59
 */
public class SendMailSSL {

    private final AuthMe plugin;
    private final NewSetting settings;

    public SendMailSSL(AuthMe plugin, NewSetting settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public void main(final PlayerAuth auth, final String newPass) {
        final String mailText = replaceMailTags(settings.getEmailMessage(), plugin, auth, newPass);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {

            @Override
            public void run() {
                Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
                HtmlEmail email;
                try {
                    email = initializeMail(auth, settings);
                } catch (EmailException e) {
                    ConsoleLogger.logException("Failed to create email with the given settings:", e);
                    return;
                }

                String content = mailText;
                // Generate an image?
                File file = null;
                if (settings.getProperty(EmailSettings.PASSWORD_AS_IMAGE)) {
                    try {
                        file = generateImage(auth, plugin, newPass);
                        content = embedImageIntoEmailContent(file, email, content);
                    } catch (IOException | EmailException e) {
                        ConsoleLogger.logException(
                            "Unable to send new password as image for email " + auth.getEmail() + ":", e);
                    }
                }

                sendEmail(content, email);
                if (file != null) {
                    file.delete();
                }
            }

        });
    }

    private static File generateImage(PlayerAuth auth, AuthMe plugin, String newPass) throws IOException {
        ImageGenerator gen = new ImageGenerator(newPass);
        File file = new File(plugin.getDataFolder(), auth.getNickname() + "_new_pass.jpg");
        ImageIO.write(gen.generateImage(), "jpg", file);
        return file;
    }

    private static String embedImageIntoEmailContent(File image, HtmlEmail email, String content)
            throws EmailException {
        DataSource source = new FileDataSource(image);
        String tag = email.embed(source, image.getName());
        return content.replace("<image />", "<img src=\"cid:" + tag + "\">");
    }

    private static HtmlEmail initializeMail(PlayerAuth auth, NewSetting settings)
            throws EmailException {
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
        email.addTo(auth.getEmail());
        email.setFrom(senderMail, senderName);
        email.setSubject(settings.getProperty(EmailSettings.RECOVERY_MAIL_SUBJECT));
        email.setAuthentication(senderMail, mailPassword);

        setPropertiesForPort(email, port, settings);
        return email;
    }

    private static boolean sendEmail(String content, HtmlEmail email) {
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

    private static String replaceMailTags(String mailText, AuthMe plugin, PlayerAuth auth, String newPass) {
        return mailText
            .replace("<playername />", auth.getNickname())
            .replace("<servername />", plugin.getServer().getServerName())
            .replace("<generatedpass />", newPass);
    }

	private static void setPropertiesForPort(HtmlEmail email, int port, NewSetting settings)
            throws EmailException {
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
                    email.setTLS(true);
                }
                break;
            case 25:
                email.setStartTLSEnabled(true);
                email.setSSLCheckServerIdentity(true);
                break;
            case 465:
                email.setSslSmtpPort(Integer.toString(port));
                email.setSSL(true);
                break;
            default:
                email.setStartTLSEnabled(true);
                email.setSSLOnConnect(true);
                email.setSSLCheckServerIdentity(true);
        }
    }
}
