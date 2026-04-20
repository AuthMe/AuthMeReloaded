package fr.xephi.authme.mail;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.FileUtils;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;

import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;

/**
 * Creates emails and sends them.
 */
public class EmailService {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(EmailService.class);

    private final File dataFolder;
    private final Settings settings;
    private final SendMailSsl sendMailSsl;

    @Inject
    EmailService(@DataFolder File dataFolder, Settings settings, SendMailSsl sendMailSsl) {
        this.dataFolder = dataFolder;
        this.settings = settings;
        this.sendMailSsl = sendMailSsl;
    }

    public boolean hasAllInformation() {
        return sendMailSsl.hasAllInformation();
    }


    /**
     * Sends an email to the user with his new password.
     *
     * @param name the name of the player
     * @param mailAddress the player's email
     * @param newPass the new password
     * @return true if email could be sent, false otherwise
     */
    public boolean sendPasswordMail(String name, String mailAddress, String newPass) {
        if (!hasAllInformation()) {
            logger.warning("Cannot perform email registration: not all email settings are complete");
            return false;
        }

        HtmlEmail email;
        try {
            email = sendMailSsl.initializeMail(mailAddress);
        } catch (EmailException e) {
            logger.logException("Failed to create email with the given settings:", e);
            return false;
        }

        String mailText = replaceTagsForPasswordMail(settings.getPasswordEmailMessage(), name, newPass);
        // Generate an image?
        File file = null;
        if (settings.getProperty(EmailSettings.PASSWORD_AS_IMAGE)) {
            try {
                file = generatePasswordImage(name, newPass);
                mailText = embedImageIntoEmailContent(file, email, mailText);
            } catch (IOException | EmailException e) {
                logger.logException(
                    "Unable to send new password as image for email " + mailAddress + ":", e);
            }
        }

        boolean couldSendEmail = sendMailSsl.sendEmail(mailText, email);
        FileUtils.delete(file);
        return couldSendEmail;
    }

    /**
     * Sends an email to the user with the temporary verification code.
     *
     * @param name the name of the player
     * @param mailAddress the player's email
     * @param code the verification code
     * @return true if email could be sent, false otherwise
     */
    public boolean sendVerificationMail(String name, String mailAddress, String code) {
        if (!hasAllInformation()) {
            logger.warning("Cannot send verification email: not all email settings are complete");
            return false;
        }

        HtmlEmail email;
        try {
            email = sendMailSsl.initializeMail(mailAddress);
        } catch (EmailException e) {
            logger.logException("Failed to create verification email with the given settings:", e);
            return false;
        }

        String mailText = replaceTagsForVerificationEmail(settings.getVerificationEmailMessage(), name, code,
            settings.getProperty(SecuritySettings.VERIFICATION_CODE_EXPIRATION_MINUTES));
        return sendMailSsl.sendEmail(mailText, email);
    }

    /**
     * Sends an email to the user with a recovery code for the password recovery process.
     *
     * @param name the name of the player
     * @param email the player's email address
     * @param code the recovery code
     * @return true if email could be sent, false otherwise
     */
    public boolean sendRecoveryCode(String name, String email, String code) {
        HtmlEmail htmlEmail;
        try {
            htmlEmail = sendMailSsl.initializeMail(email);
        } catch (EmailException e) {
            logger.logException("Failed to create email for recovery code:", e);
            return false;
        }

        String message = replaceTagsForRecoveryCodeMail(settings.getRecoveryCodeEmailMessage(),
            name, code, settings.getProperty(SecuritySettings.RECOVERY_CODE_HOURS_VALID));
        return sendMailSsl.sendEmail(message, htmlEmail);
    }

    private File generatePasswordImage(String name, String newPass) throws IOException {
        ImageGenerator gen = new ImageGenerator(newPass);
        File file = new File(dataFolder, name + "_new_pass.jpg");
        ImageIO.write(gen.generateImage(), "jpg", file);
        return file;
    }

    private static String embedImageIntoEmailContent(File image, HtmlEmail email, String content)
        throws EmailException {
        DataSource source = new FileDataSource(image);
        String tag = email.embed(source, image.getName());
        return content.replace("<image />", "<img src=\"cid:" + tag + "\">");
    }

    private String replaceTagsForPasswordMail(String mailText, String name, String newPass) {
        return mailText
            .replace("<playername />", name)
            .replace("<servername />", settings.getProperty(PluginSettings.SERVER_NAME))
            .replace("<generatedpass />", newPass);
    }

    private String replaceTagsForVerificationEmail(String mailText, String name, String code, int minutesValid) {
        return mailText
            .replace("<playername />", name)
            .replace("<servername />", settings.getProperty(PluginSettings.SERVER_NAME))
            .replace("<generatedcode />", code)
            .replace("<minutesvalid />", String.valueOf(minutesValid));
    }

    private String replaceTagsForRecoveryCodeMail(String mailText, String name, String code, int hoursValid) {
        return mailText
            .replace("<playername />", name)
            .replace("<servername />", settings.getProperty(PluginSettings.SERVER_NAME))
            .replace("<recoverycode />", code)
            .replace("<hoursvalid />", String.valueOf(hoursValid));
    }
}
