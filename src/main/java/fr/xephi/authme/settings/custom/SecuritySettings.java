package fr.xephi.authme.settings.custom;

import fr.xephi.authme.settings.custom.domain.Comment;
import fr.xephi.authme.settings.custom.domain.Property;
import fr.xephi.authme.settings.custom.domain.SettingsClass;

import static fr.xephi.authme.settings.custom.domain.Property.newProperty;
import static fr.xephi.authme.settings.custom.domain.PropertyType.BOOLEAN;
import static fr.xephi.authme.settings.custom.domain.PropertyType.INTEGER;

public class SecuritySettings implements SettingsClass {

	@Comment({"Stop the server if we can't contact the sql database",
        "Take care with this, if you set this to false,",
        "AuthMe will automatically disable and the server won't be protected!"})
	public static final Property<Boolean> STOP_SERVER_ON_PROBLEM =
        newProperty(BOOLEAN, "Security.SQLProblem.stopServer", true);

	@Comment("/reload support")
    public static final Property<Boolean> USE_RELOAD_COMMAND_SUPPORT =
        newProperty(BOOLEAN, "Security.ReloadCommand.useReloadCommandSupport", true);

	@Comment("Remove spam from console?")
    public static final Property<Boolean> REMOVE_SPAM_FROM_CONSOLE =
        newProperty(BOOLEAN, "Security.console.noConsoleSpam", false);

	@Comment("Remove passwords from console?")
    public static final Property<Boolean> REMOVE_PASSWORD_FROM_CONSOLE =
        newProperty(BOOLEAN, "Security.console.removePassword", true);

	@Comment("Player need to put a captcha when he fails too lot the password")
    public static final Property<Boolean> USE_CAPTCHA =
        newProperty(BOOLEAN, "Security.captcha.useCaptcha", false);

	@Comment("Max allowed tries before request a captcha")
    public static final Property<Integer> MAX_LOGIN_TRIES_BEFORE_CAPTCHA =
        newProperty(INTEGER, "Security.captcha.maxLoginTry", 5);

	@Comment("Captcha length")
    public static final Property<Integer> CAPTCHA_LENGTH =
        newProperty(INTEGER, "Security.captcha.captchaLength", 5);

    @Comment({"Kick players before stopping the server, that allow us to save position of players",
        "and all needed information correctly without any corruption."})
    public static final Property<Boolean> KICK_PLAYERS_BEFORE_STOPPING =
        newProperty(BOOLEAN, "Security.stop.kickPlayersBeforeStopping", true);

    private SecuritySettings() {
    }

}
