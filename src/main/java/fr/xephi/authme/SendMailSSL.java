package fr.xephi.authme;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

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

        String port = String.valueOf(Settings.getMailPort);
        Properties props = new Properties();
        props.put("mail.smtp.host", Settings.getmailSMTP);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.starttls.enable", true);

        try {
            Session session = Session.getInstance(props, null);

            final Message message = new MimeMessage(session);
            try {
                message.setFrom(new InternetAddress(Settings.getmailAccount, sendername));
            } catch (UnsupportedEncodingException uee) {
                message.setFrom(new InternetAddress(Settings.getmailAccount));
            }
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(auth.getEmail()));
            message.setSubject(Settings.getMailSubject);
            message.setSentDate(new Date());
            BodyPart messageBodyPart = new MimeBodyPart();
            String text = Settings.getMailText;
            messageBodyPart.setText(text);

            Multipart multipart = new MimeMultipart();

            multipart.addBodyPart(messageBodyPart);

            messageBodyPart = new MimeBodyPart();

            multipart.addBodyPart(messageBodyPart);
            message.setContent(multipart);
            final Transport transport = session.getTransport("smtp");
            transport.connect(Settings.getmailSMTP, Settings.getmailAccount, Settings.getmailPassword);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {

                @Override
                public void run() {
                    try {
                        transport.sendMessage(message, message.getAllRecipients());
                    } catch (MessagingException e) {
                        System.out.println("Some error occured while trying to send a mail to " + auth.getEmail());
                    }
                }

            });
        } catch (Exception e) {
            System.out.println("Some error occured while trying to send a mail to " + auth.getEmail());
        }
    }
}
