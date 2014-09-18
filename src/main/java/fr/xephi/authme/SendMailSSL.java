package fr.xephi.authme;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.bukkit.Bukkit;

import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.settings.Settings;

/**
 *
 * @author Xephi59
 */
public class SendMailSSL {

    public AuthMe plugin;

    public SendMailSSL(AuthMe plugin) {
        this.plugin = plugin;
    }

    public void main(final PlayerAuth auth, final String newPass) {
        String sendername;

        if (Settings.getmailSenderName.isEmpty() || Settings.getmailSenderName == null) {
            sendername = Settings.getmailAccount;
        } else {
            sendername = Settings.getmailSenderName;
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", Settings.getmailSMTP);
        props.put("mail.smtp.socketFactory.port", String.valueOf(Settings.getMailPort));
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", String.valueOf(Settings.getMailPort));

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(Settings.getmailAccount, Settings.getmailPassword);
            }
        });

        try {
            final Message message = new MimeMessage(session);
            try {
                message.setFrom(new InternetAddress(Settings.getmailAccount, sendername));
            } catch (UnsupportedEncodingException uee) {
                message.setFrom(new InternetAddress(Settings.getmailAccount));
            }
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(auth.getEmail()));
            message.setSubject(Settings.getMailSubject);
            message.setSentDate(new Date());
            String text = Settings.getMailText;
            text = text.replace("<playername>", auth.getNickname());
            text = text.replace("<servername>", plugin.getServer().getServerName());
            text = text.replace("<generatedpass>", newPass);
            message.setContent(text, "text/html");
            Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                @Override
                public void run() {
                    try {
                        Transport.send(message);
                    } catch (MessagingException e) {
                        e.printStackTrace();
                    }
                }
            });
            if (!Settings.noConsoleSpam)
                ConsoleLogger.info("Email sent to : " + auth.getNickname());
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
