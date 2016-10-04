package fr.xephi.authme.util;

import fr.xephi.authme.TestHelper;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.regex.Pattern;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link Utils}.
 */
public class UtilsTest {

    @BeforeClass
    public static void setAuthmeInstance() {
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
    public void shouldCheckIfClassIsLoaded() {
        // given / when / then
        assertThat(Utils.isClassLoaded("org.bukkit.event.player.PlayerFishEvent"), equalTo(true));
        assertThat(Utils.isClassLoaded("com.someclass.doesnot.exist"), equalTo(false));
    }
}
