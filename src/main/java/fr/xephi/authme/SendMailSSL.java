package fr.xephi.authme;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.imageio.ImageIO;
import javax.mail.BodyPart;
import javax.mail.Message;
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

        if (Settings.getmailSenderName == null || Settings.getmailSenderName.isEmpty()) {
            sendername = Settings.getmailAccount;
        } else {
            sendername = Settings.getmailSenderName;
        }

        final String sender = sendername;
        final String port = String.valueOf(Settings.getMailPort);
        final String acc = Settings.getmailAccount;
        final String subject = Settings.getMailSubject;
        final String smtp = Settings.getmailSMTP;
        final String password = Settings.getmailPassword;
        final String mailText = Settings.getMailText.replace("<playername>", auth.getNickname()).replace("<servername>", plugin.getServer().getServerName()).replace("<generatedpass>", newPass);
        final String mail = auth.getEmail();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {

            @Override
            public void run() {
                try {
                    Properties props = new Properties();
                    props.put("mail.smtp.host", smtp);
                    props.put("mail.smtp.auth", "true");
                    props.put("mail.smtp.port", port);
                    props.put("mail.smtp.starttls.enable", true);
                    Session session = Session.getInstance(props, null);

                    Message message = new MimeMessage(session);
                    try {
                        message.setFrom(new InternetAddress(acc, sender));
                    } catch (UnsupportedEncodingException uee) {
                        message.setFrom(new InternetAddress(acc));
                    }
                    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mail));
                    message.setSubject(subject);
                    message.setSentDate(new Date());
                    BodyPart messageBodyPart = new MimeBodyPart();
                    messageBodyPart.setContent(mailText, "text/html");
                    Multipart multipart = new MimeMultipart();
                    multipart.addBodyPart(messageBodyPart);

                    // Generate an image ?
                    File file = null;
                    if (Settings.generateImage) {
                        try {
                            ImageGenerator gen = new ImageGenerator(newPass);
                            file = new File(plugin.getDataFolder() + File.separator + auth.getNickname() + "_new_pass.jpg");
                            ImageIO.write(gen.generateImage(), "jpg", file);
                            messageBodyPart = new MimeBodyPart();
                            DataSource source = new FileDataSource(file);
                            messageBodyPart.setDataHandler(new DataHandler(source));
                            messageBodyPart.setFileName(auth.getNickname() + "_new_pass.jpg");
                            multipart.addBodyPart(messageBodyPart);
                        } catch (Exception e) {
                            ConsoleLogger.showError("Unable to send new password as image! Using normal text! Dest: " + mail);
                        }
                    }
                    
                    Transport transport = session.getTransport("smtp");
                    message.setContent(multipart);

                    try {
                        transport.connect(smtp, acc, password);
                    } catch (Exception e) {
                        ConsoleLogger.showError("Can't connect to your SMTP server! Aborting! Can't send recorvery email to " + mail);
                        if (file != null)
                            file.delete();
                        return;
                    }
                    transport.sendMessage(message, message.getAllRecipients());

                    if (file != null)
                        file.delete();

                } catch (RuntimeException e) {
                    ConsoleLogger.showError("Some error occured while trying to send a email to " + mail);
                } catch (Exception e) {
                    ConsoleLogger.showError("Some error occured while trying to send a email to " + mail);
                }
            }

        });
    }
}
