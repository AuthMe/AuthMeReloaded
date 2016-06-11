package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.DataSourceType;
import fr.xephi.authme.initialization.AuthMeServiceInitializer;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.bukkit.command.CommandSender;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link ReloadCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ReloadCommandTest {

    @InjectMocks
    private ReloadCommand command;

    @Mock
    private AuthMe authMe;

    @Mock
    private AuthMeServiceInitializer initializer;

    @Mock
    private NewSetting settings;

    @Mock
    private DataSource dataSource;

    @Mock
    private CommandService commandService;

    @BeforeClass
    public static void setUpLogger() {
        TestHelper.setupLogger();
    }

    @Before
    public void setDefaultSettings() {
        // Mock properties retrieved by ConsoleLogger
        given(settings.getProperty(SecuritySettings.REMOVE_SPAM_FROM_CONSOLE)).willReturn(false);
        given(settings.getProperty(SecuritySettings.USE_LOGGING)).willReturn(false);
    }

    @Test
    public void shouldReload() {
        // given
        CommandSender sender = mock(CommandSender.class);
        given(settings.getProperty(DatabaseSettings.BACKEND)).willReturn(DataSourceType.MYSQL);
        given(dataSource.getType()).willReturn(DataSourceType.MYSQL);

        // when
        command.executeCommand(sender, Collections.<String>emptyList());

        // then
        verify(settings).reload();
        verify(initializer).performReloadOnServices();
        verify(commandService).send(sender, MessageKey.CONFIG_RELOAD_SUCCESS);
    }

    @Test
    public void shouldHandleReloadError() {
        // given
        CommandSender sender = mock(CommandSender.class);
        doThrow(IllegalStateException.class).when(initializer).performReloadOnServices();
        given(settings.getProperty(DatabaseSettings.BACKEND)).willReturn(DataSourceType.MYSQL);
        given(dataSource.getType()).willReturn(DataSourceType.MYSQL);

        // when
        command.executeCommand(sender, Collections.<String>emptyList());

        // then
        verify(settings).reload();
        verify(initializer).performReloadOnServices();
        verify(sender).sendMessage(matches("Error occurred.*"));
        verify(authMe).stopOrUnload();
    }

    @Test
    public void shouldIssueWarningForChangedDatasourceSetting() {
        // given
        CommandSender sender = mock(CommandSender.class);
        given(settings.getProperty(DatabaseSettings.BACKEND)).willReturn(DataSourceType.MYSQL);
        given(dataSource.getType()).willReturn(DataSourceType.SQLITE);

        // when
        command.executeCommand(sender, Collections.<String>emptyList());

        // then
        verify(settings).reload();
        verify(initializer).performReloadOnServices();
        verify(sender).sendMessage(argThat(containsString("cannot change database type")));
    }
}
