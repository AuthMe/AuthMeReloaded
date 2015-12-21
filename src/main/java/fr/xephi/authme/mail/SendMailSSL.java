package fr.xephi.authme.mail;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.ImageGenerator;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.settings.Settings;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.bukkit.Bukkit;

import com.sun.mail.smtp.SMTPTransport;

import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.imageio.ImageIO;
import javax.mail.Transport;

import java.io.File;
import java.security.Provider;
import java.security.Security;

/**
 * @author Xephi59
 * @version $Revision: 1.0 $
 */
public class SendMailSSL {

    public final AuthMe plugin;

    /**
     * Constructor for SendMailSSL.
     *
     * @param plugin AuthMe
     */
    public SendMailSSL(AuthMe plugin) {
        this.plugin = plugin;
    }

    /**
     * Method main.
     *
     * @param auth    PlayerAuth
     * @param newPass String
     */
    public void main(final PlayerAuth auth, final String newPass) {
        String senderName;

        if (Settings.getmailSenderName == null || Settings.getmailSenderName.isEmpty()) {
            senderName = Settings.getmailAccount;
        } else {
            senderName = Settings.getmailSenderName;
        }

        final String sender = senderName;
        final int port = Settings.getMailPort;
        final String acc = Settings.getmailAccount;
        final String subject = Settings.getMailSubject;
        final String smtp = Settings.getmailSMTP;
        final String password = Settings.getmailPassword;
        final String mailText = Settings.getMailText.replace("<playername />", auth.getNickname()).replace("<servername />", plugin.getServer().getServerName()).replace("<generatedpass />", newPass);
        final String mail = auth.getEmail();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
                    HtmlEmail email = new HtmlEmail();
                    email.setSmtpPort(port);
                    email.setHostName(smtp);
                    email.addTo(mail);
                    email.setFrom(acc, sender);
                    email.setSubject(subject);
                    if (acc != null && !acc.isEmpty() && password != null && !password.isEmpty())
                    	email.setAuthenticator(new DefaultAuthenticator(acc, !Settings.emailOauth2Token.isEmpty() ? "" : password));
                    switch (port) {
                    	case 587:
                    		email.setStartTLSEnabled(true);
                    		email.setStartTLSRequired(true);
                    		if (!Settings.emailOauth2Token.isEmpty())
                    		{
                    			if (Security.getProvider("Google OAuth2 Provider") == null)
                    				Security.addProvider(new OAuth2Provider());
                    			email.getMailSession().getProperties().setProperty("mail.smtp.starttls.enable", "true");
                    			email.getMailSession().getProperties().setProperty("mail.smtp.starttls.required", "true");
                    			email.getMailSession().getProperties().setProperty("mail.smtp.sasl.enable", "true");
                    			email.getMailSession().getProperties().setProperty("mail.smtp.sasl.mechanisms", "XOAUTH2");
                    			email.getMailSession().getProperties().setProperty(OAuth2SaslClientFactory.OAUTH_TOKEN_PROP, password);
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
                    		break;
                    }
                    String content = mailText;
                    // Generate an image ?
                    File file = null;
                    if (Settings.generateImage) {
                        try {
                            ImageGenerator gen = new ImageGenerator(newPass);
                            file = new File(plugin.getDataFolder() + File.separator + auth.getNickname() + "_new_pass.jpg");
                            ImageIO.write(gen.generateImage(), "jpg", file);
                            DataSource source = new FileDataSource(file);
                            String tag = email.embed(source, auth.getNickname() + "_new_pass.jpg");
                            content = content.replace("%image%", "<img src=\"cid:" + tag + "\">");
                        } catch (Exception e) {
                            ConsoleLogger.showError("Unable to send new password as image! Using normal text! Dest: " + mail);
                        }
                    }
                    try {
                        email.setHtmlMsg(content);
                        email.setTextMsg(content);
                    } catch (EmailException e)
                    {
                    	ConsoleLogger.showError("Your email.html config contains some error and cannot be send!");
                    	return;
                    }
                    try {
                    	email.send();
                    } catch (Exception e) {
                        ConsoleLogger.showError("Fail to send a mail to " + mail + " cause " + e.getLocalizedMessage());
                    }
                    if (file != null)
                        //noinspection ResultOfMethodCallIgnored
                        file.delete();

                } catch (Exception e) {
                    // Print the stack trace
                    e.printStackTrace();
                    ConsoleLogger.showError("Some error occurred while trying to send a email to " + mail);
                }
            }

        });
    }
}
