package fr.xephi.authme.mail;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.output.LogLevel;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.util.StringUtils;
import org.apache.commons.mail.EmailConstants;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.inject.Inject;
import javax.mail.Session;
import java.security.Security;
import java.util.Properties;

import static fr.xephi.authme.settings.properties.EmailSettings.MAIL_ACCOUNT;
import static fr.xephi.authme.settings.properties.EmailSettings.MAIL_PASSWORD;


/**
 * Sends emails to players on behalf of the server.
 */
public class SendMailSsl {

    private ConsoleLogger logger = ConsoleLoggerFactory.get(SendMailSsl.class);

    @Inject
    private Settings settings;

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
     * Creates a {@link HtmlEmail} object configured as per the AuthMe config
     * with the given email address as recipient.
     *
     * @param emailAddress the email address the email is destined for
     * @return the created HtmlEmail object
     * @throws EmailException if the mail is invalid
     */
    public HtmlEmail initializeMail(String emailAddress) throws EmailException {
        String senderMail = StringUtils.isBlank(settings.getProperty(EmailSettings.MAIL_ADDRESS))
            ? settings.getProperty(EmailSettings.MAIL_ACCOUNT)
            : settings.getProperty(EmailSettings.MAIL_ADDRESS);

        String senderName = StringUtils.isBlank(settings.getProperty(EmailSettings.MAIL_SENDER_NAME))
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
        email.setAuthentication(settings.getProperty(EmailSettings.MAIL_ACCOUNT), mailPassword);
        if (settings.getProperty(PluginSettings.LOG_LEVEL).includes(LogLevel.DEBUG)) {
            email.setDebug(true);
        }

        setPropertiesForPort(email, port);
        return email;
    }

    /**
     * Sets the given content to the HtmlEmail object and sends it.
     *
     * @param content the content to set
     * @param email the email object to send
     * @return true upon success, false otherwise
     */
    public boolean sendEmail(String content, HtmlEmail email) {
        Thread.currentThread().setContextClassLoader(SendMailSsl.class.getClassLoader());
        // Issue #999: Prevent UnsupportedDataTypeException: no object DCH for MIME type multipart/alternative
        // cf. http://stackoverflow.com/questions/21856211/unsupporteddatatypeexception-no-object-dch-for-mime-type
        MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
        mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
        mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
        mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
        mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
        mc.addMailcap("message/rfc822;; x-java-content- handler=com.sun.mail.handlers.message_rfc822");

        try {
            email.setHtmlMsg(content);
            email.setTextMsg(content);
        } catch (EmailException e) {
            logger.logException("Your email.html config contains an error and cannot be sent:", e);
            return false;
        }
        try {
            email.send();
            return true;
        } catch (EmailException e) {
            logger.logException("Failed to send a mail to " + email.getToAddresses() + ":", e);
            return false;
        }
    }

    /**
     * Sets properties to the given HtmlEmail object based on the port from which it will be sent.
     *
     * @param email the email object to configure
     * @param port the configured outgoing port
     */
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
                if (settings.getProperty(EmailSettings.PORT25_USE_TLS)) {
                    email.setStartTLSEnabled(true);
                    email.setSSLCheckServerIdentity(true);
                }
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
