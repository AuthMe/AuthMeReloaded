package fr.xephi.authme.util;

import fr.xephi.authme.TestHelper;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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
        Pattern result = Utils.safePatternCompile(pattern, patternString -> null);

        // then
        assertThat(result.toString(), equalTo(pattern));
    }

    @Test
    public void shouldDefaultToAllAllowedPattern() {
        // given
        String invalidPattern = "gr(a|eys?"; // missing closing ')'

        // when
        Pattern result = Utils.safePatternCompile(invalidPattern, patternString -> Utils.MATCH_ANYTHING_PATTERN);

        // then
        assertThat(result.toString(), equalTo(".*?"));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void shouldCheckIfCollectionIsEmpty() {
        // given
        List<String> emptyList = Collections.emptyList();
        Collection<Integer> nonEmptyColl = Arrays.asList(3, 4, 5);

        // when / then
        assertThat(Utils.isCollectionEmpty(emptyList), equalTo(true));
        assertThat(Utils.isCollectionEmpty(nonEmptyColl), equalTo(false));
        assertThat(Utils.isCollectionEmpty(null), equalTo(true));
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
