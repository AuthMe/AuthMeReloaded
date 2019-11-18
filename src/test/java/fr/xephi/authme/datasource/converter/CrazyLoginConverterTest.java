package fr.xephi.authme.datasource.converter;

import ch.jalu.injector.testing.DelayedInjectionRunner;
import ch.jalu.injector.testing.InjectDelayed;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.ConverterSettings;
import org.bukkit.command.CommandSender;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.File;
import java.util.List;

import static fr.xephi.authme.AuthMeMatchers.equalToHash;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Test for {@link CrazyLoginConverter}.
 */
@RunWith(DelayedInjectionRunner.class)
public class CrazyLoginConverterTest {

    @InjectDelayed
    private CrazyLoginConverter crazyLoginConverter;

    @Mock
    private DataSource dataSource;

    @Mock
    private Settings settings;

    @DataFolder
    private File dataFolder = TestHelper.getJarFile(TestHelper.PROJECT_ROOT + "/datasource/converter/");

    @BeforeClass
    public static void initializeLogger() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldImportUsers() {
        // given
        given(settings.getProperty(ConverterSettings.CRAZYLOGIN_FILE_NAME)).willReturn("crazylogin.db");
        CommandSender sender = mock(CommandSender.class);

        // when
        crazyLoginConverter.execute(sender);

        // then
        ArgumentCaptor<PlayerAuth> authCaptor = ArgumentCaptor.forClass(PlayerAuth.class);
        verify(dataSource, times(2)).saveAuth(authCaptor.capture());
        List<PlayerAuth> savedAuths = authCaptor.getAllValues();
        assertNameAndRealName(savedAuths.get(0), "qotato", "qotaTo");
        assertThat(savedAuths.get(0).getPassword(), equalToHash("8267663ab198a96437b9f455429a2c1b6c943111613c217bf2703c14d08a309d34e510ddb5549507b1500759dbcf9d4a99bc765ff37b32bd31adbb1e92e74ac5"));
        assertNameAndRealName(savedAuths.get(1), "bobby", "Bobby");
        assertThat(savedAuths.get(1).getPassword(), equalToHash("ad50dbc841e6321210530801f5219a5ffb4c7c41f11878d465374a4b8db2965c50f69b6098918a58e4adea312e3633c7724b15e24a217009e6fa2b3c299d55f2"));
    }

    @Test
    public void shouldStopForNonExistentFile() {
        // given
        given(settings.getProperty(ConverterSettings.CRAZYLOGIN_FILE_NAME)).willReturn("invalid-file");
        CommandSender sender = mock(CommandSender.class);

        // when
        crazyLoginConverter.execute(sender);

        // then
        verifyNoInteractions(dataSource);
        verify(sender).sendMessage(argThat(containsString("file not found")));
    }

    private static void assertNameAndRealName(PlayerAuth auth, String name, String realName) {
        assertThat(auth.getNickname(), equalTo(name));
        assertThat(auth.getRealName(), equalTo(realName));
    }

}
