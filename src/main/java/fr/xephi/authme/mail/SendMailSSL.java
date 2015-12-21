package fr.xephi.authme.mail;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.ImageGenerator;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.settings.Settings;

import fr.xephi.authme.util.StringUtils;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.bukkit.Bukkit;

import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.security.Security;
import java.util.Properties;

/**
 * @author Xephi59
 */
public class SendMailSSL {

    private final AuthMe plugin;

    public SendMailSSL(AuthMe plugin) {
        this.plugin = plugin;
    }

    public void main(final PlayerAuth auth, final String newPass) {
        final String mailText = replaceMailTags(Settings.getMailText, plugin, auth, newPass);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {

            @Override
            public void run() {
                Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
                HtmlEmail email;
                try {
                    email = initializeMail(auth);
                } catch (EmailException e) {
                    ConsoleLogger.showError("Failed to create email with the given settings: "
                        + StringUtils.formatException(e));
                    return;
                }

                String content = mailText;
                // Generate an image?
                File file = null;
                if (Settings.generateImage) {
                    try {
                        file = generateImage(auth, plugin, newPass);
                        content = embedImageIntoEmailContent(file, email, content);
                    } catch (IOException | EmailException e) {
                        ConsoleLogger.showError("Unable to send new password as image for email "
                            + auth.getEmail() + ": " + StringUtils.formatException(e));
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
        File file = new File(plugin.getDataFolder() + File.separator + auth.getNickname() + "_new_pass.jpg");
        ImageIO.write(gen.generateImage(), "jpg", file);
        return file;
    }

    private static String embedImageIntoEmailContent(File image, HtmlEmail email, String content)
        throws EmailException {
        DataSource source = new FileDataSource(image);
        String tag = email.embed(source, image.getName());
        return content.replace("<image />", "<img src=\"cid:" + tag + "\">");
    }

    private static HtmlEmail initializeMail(PlayerAuth auth) throws EmailException {
        String senderName;
        if (StringUtils.isEmpty(Settings.getmailSenderName)) {
            senderName = Settings.getmailAccount;
        } else {
            senderName = Settings.getmailSenderName;
        }
        String senderMail = Settings.getmailAccount;
        String mailPassword = Settings.getmailPassword;
        int port = Settings.getMailPort;

        HtmlEmail email = new HtmlEmail();
        email.setSmtpPort(port);
        email.setHostName(Settings.getmailSMTP);
        email.addTo(auth.getEmail());
        email.setFrom(senderMail, senderName);
        email.setSubject(Settings.getMailSubject);
        if (!StringUtils.isEmpty(senderMail) && !StringUtils.isEmpty(mailPassword)) {
            String password = !Settings.emailOauth2Token.isEmpty() ? "" : mailPassword;
            email.setAuthenticator(new DefaultAuthenticator(senderMail, password));
        }

        setPropertiesForPort(email, port);
        return email;
    }

    private static boolean sendEmail(String content, HtmlEmail email) {
        try {
            email.setHtmlMsg(content);
            email.setTextMsg(content);
        } catch (EmailException e) {
            ConsoleLogger.showError("Your email.html config contains an error and cannot be sent: "
                + StringUtils.formatException(e));
            return false;
        }
        try {
            email.send();
            return true;
        } catch (EmailException e) {
            ConsoleLogger.showError("Failed to send a mail to " + email.getToAddresses() + ": " + e.getMessage());
            return false;
        }
    }

    private static String replaceMailTags(String mailText, AuthMe plugin, PlayerAuth auth, String newPass) {
        return mailText
            .replace("<playername />", auth.getNickname())
            .replace("<servername />", plugin.getServer().getServerName())
            .replace("<generatedpass />", newPass);
    }

    private static void setPropertiesForPort(HtmlEmail email, int port) throws EmailException {
        switch (port) {
            case 587:
                email.setStartTLSEnabled(true);
                email.setStartTLSRequired(true);
                if (!Settings.emailOauth2Token.isEmpty()) {
                    if (Security.getProvider("Google OAuth2 Provider") == null) {
                        Security.addProvider(new OAuth2Provider());
                    }

                    Properties mailProperties = email.getMailSession().getProperties();
                    mailProperties.setProperty("mail.smtp.starttls.enable", "true");
                    mailProperties.setProperty("mail.smtp.starttls.required", "true");
                    mailProperties.setProperty("mail.smtp.sasl.enable", "true");
                    mailProperties.setProperty("mail.smtp.sasl.mechanisms", "XOAUTH2");
                    mailProperties.setProperty(OAuth2SaslClientFactory.OAUTH_TOKEN_PROP, Settings.getmailPassword);
                }
                break;
            case 25:
                email.setStartTLSEnabled(true);
                email.setSSLCheckServerIdentity(true);
                break;
            case 465:
                email.setSSLOnConnect(true);
                email.setSSLCheckServerIdentity(true);
                break;
            default:
                email.setStartTLSEnabled(true);
                email.setSSLOnConnect(true);
                email.setSSLCheckServerIdentity(true);
        }
    }
}
