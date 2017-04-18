package fr.xephi.authme.util;

import fr.xephi.authme.TestHelper;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.logging.Logger;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link Utils}.
 */
public class UtilsTest {

    @BeforeClass
    public static void initLogger() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldCompilePattern() {
        // given
        String pattern = "gr(a|e)ys?";

        // when
        Pattern result = Utils.safePatternCompile(pattern);

        // then
        assertThat(result.toString(), equalTo(pattern));
    }

    @Test
    public void shouldDefaultToAllAllowedPattern() {
        // given
        String invalidPattern = "gr(a|eys?"; // missing closing ')'

        // when
        Pattern result = Utils.safePatternCompile(invalidPattern);

        // then
        assertThat(result.toString(), equalTo(".*?"));
    }

    @Test
    public void shouldHavePrivateConstructorOnly() {
        // given / when / then
        TestHelper.validateHasOnlyPrivateEmptyConstructor(Utils.class);
    }

    @Test
    public void shouldLogAndSendMessage() {
        // given
        Logger logger = TestHelper.setupLogger();
        Player player = mock(Player.class);
        String message = "Finished adding foo to the bar";

        // when
        Utils.logAndSendMessage(player, message);

        // then
        verify(logger).info(message);
        verify(player).sendMessage(message);
    }

    @Test
    public void shouldHandleNullAsCommandSender() {
        // given
        Logger logger = TestHelper.setupLogger();
        String message = "Test test, test.";

        // when
        Utils.logAndSendMessage(null, message);

        // then
        verify(logger).info(message);
    }

    @Test
    public void shouldNotSendToCommandSenderTwice() {
        // given
        Logger logger = TestHelper.setupLogger();
        CommandSender sender = mock(ConsoleCommandSender.class);
        String message = "Test test, test.";

        // when
        Utils.logAndSendMessage(sender, message);

        // then
        verify(logger).info(message);
        verifyZeroInteractions(sender);
    }

    @Test
    public void shouldCheckIfClassIsLoaded() {
        // given / when / then
        assertThat(Utils.isClassLoaded("org.bukkit.event.player.PlayerFishEvent"), equalTo(true));
        assertThat(Utils.isClassLoaded("com.someclass.doesnot.exist"), equalTo(false));
    }

    @Test
    public void shouldDetectIfEmailIsEmpty() {
        // given / when / then
        assertThat(Utils.isEmailEmpty(""), equalTo(true));
        assertThat(Utils.isEmailEmpty(null), equalTo(true));
        assertThat(Utils.isEmailEmpty("your@email.com"), equalTo(true));
        assertThat(Utils.isEmailEmpty("Your@Email.com"), equalTo(true));

        assertThat(Utils.isEmailEmpty("my@example.org"), equalTo(false));
        assertThat(Utils.isEmailEmpty("hey"), equalTo(false));
    }
}
