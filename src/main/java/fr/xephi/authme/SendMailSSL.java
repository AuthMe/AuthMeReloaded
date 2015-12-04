package fr.xephi.authme;

import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.settings.Settings;
import org.apache.commons.mail.HtmlEmail;
import org.bukkit.Bukkit;

import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.imageio.ImageIO;
import java.io.File;

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
        final String mailText = Settings.getMailText.replace("%playername%", auth.getNickname()).replace("%servername%", plugin.getServer().getServerName()).replace("%generatedpass%", newPass);
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
                    email.setAuthentication(acc, password);
                    email.setStartTLSEnabled(true);
                    email.setStartTLSRequired(true);
                    email.setSSLCheckServerIdentity(true);
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
                    email.setHtmlMsg(content);
                    email.setTextMsg(content);
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
