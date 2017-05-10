package fr.xephi.authme.datasource.converter;

import ch.jalu.injector.testing.BeforeInjecting;
import ch.jalu.injector.testing.DelayedInjectionRunner;
import ch.jalu.injector.testing.InjectDelayed;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.ConverterSettings;
import org.bukkit.command.CommandSender;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import static fr.xephi.authme.AuthMeMatchers.equalToHash;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link LoginSecurityConverter}.
 */
@RunWith(DelayedInjectionRunner.class)
public class LoginSecurityConverterTest {

    @InjectDelayed
    private LoginSecurityConverter converter;

    @Mock
    private DataSource dataSource;
    @Mock
    private Settings settings;
    @DataFolder
    private File dataFolder = new File("."); // not used but required for injection

    @BeforeInjecting
    public void initMocks() {
        TestHelper.setupLogger();
        given(settings.getProperty(ConverterSettings.LOGINSECURITY_USE_SQLITE)).willReturn(true);
    }

    @Test
    public void shouldConvertFromSqlite() throws SQLException {
        // given
        Connection connection = converter.createSqliteConnection(
            TestHelper.TEST_RESOURCES_FOLDER + TestHelper.PROJECT_ROOT + "datasource/converter/LoginSecurity.db");
        CommandSender sender = mock(CommandSender.class);

        // when
        converter.performConversion(sender, connection);

        // then
        ArgumentCaptor<PlayerAuth> captor = ArgumentCaptor.forClass(PlayerAuth.class);
        verify(dataSource, times(3)).saveAuth(captor.capture());
        assertThat(captor.getAllValues().get(0).getNickname(), equalTo("player1"));
        assertThat(captor.getAllValues().get(0).getRealName(), equalTo("Player1"));
        assertThat(captor.getAllValues().get(0).getLastLogin(), equalTo(1494242093652L));
        assertThat(captor.getAllValues().get(0).getPassword(), equalToHash("$2a$10$E1Ri7XKeIIBv4qVaiPplgepT7QH9xGFh3hbHfcmCjq7hiW.UBTiGK"));
        assertThat(captor.getAllValues().get(0).getIp(), equalTo("127.0.0.1"));

        assertThat(captor.getAllValues().get(1).getNickname(), equalTo("player2"));
        assertThat(captor.getAllValues().get(1).getLastLogin(), equalTo(1494242174589L));
        assertThat(captor.getAllValues().get(1).getIp(), equalTo("127.4.5.6"));

        assertThat(captor.getAllValues().get(2).getRealName(), equalTo("Player3"));
        assertThat(captor.getAllValues().get(2).getPassword(), equalToHash("$2a$10$WFui8KSXMLDOVXKFpCLyPukPi4M82w1cv/rNojsAnwJjba3pp8sba"));
    }

}
