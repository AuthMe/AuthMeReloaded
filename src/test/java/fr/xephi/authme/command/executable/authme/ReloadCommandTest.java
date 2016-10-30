package fr.xephi.authme.command.executable.authme;

import ch.jalu.injector.Injector;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.DataSourceType;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.output.LogLevel;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.bukkit.command.CommandSender;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

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
    private Injector injector;

    @Mock
    private Settings settings;

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
        given(settings.getProperty(PluginSettings.LOG_LEVEL)).willReturn(LogLevel.INFO);
        given(settings.getProperty(SecuritySettings.USE_LOGGING)).willReturn(false);
    }

    @Test
    public void shouldReload() {
        // given
        CommandSender sender = mock(CommandSender.class);
        given(settings.getProperty(DatabaseSettings.BACKEND)).willReturn(DataSourceType.MYSQL);
        given(dataSource.getType()).willReturn(DataSourceType.MYSQL);
        List<Reloadable> reloadables = Arrays.asList(
            mock(Reloadable.class), mock(Reloadable.class), mock(Reloadable.class));
        List<SettingsDependent> dependents = Arrays.asList(
            mock(SettingsDependent.class), mock(SettingsDependent.class));
        given(injector.retrieveAllOfType(Reloadable.class)).willReturn(reloadables);
        given(injector.retrieveAllOfType(SettingsDependent.class)).willReturn(dependents);

        // when
        command.executeCommand(sender, Collections.<String>emptyList());

        // then
        verify(settings).reload();
        verifyReloadingCalls(reloadables, dependents);
        verify(commandService).send(sender, MessageKey.CONFIG_RELOAD_SUCCESS);
    }

    @Test
    public void shouldHandleReloadError() {
        // given
        CommandSender sender = mock(CommandSender.class);
        doThrow(IllegalStateException.class).when(injector).retrieveAllOfType(Reloadable.class);
        given(settings.getProperty(DatabaseSettings.BACKEND)).willReturn(DataSourceType.MYSQL);
        given(dataSource.getType()).willReturn(DataSourceType.MYSQL);

        // when
        command.executeCommand(sender, Collections.<String>emptyList());

        // then
        verify(settings).reload();
        verify(injector).retrieveAllOfType(Reloadable.class);
        verify(sender).sendMessage(argThat(containsString("Error occurred")));
        verify(authMe).stopOrUnload();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldIssueWarningForChangedDatasourceSetting() {
        // given
        CommandSender sender = mock(CommandSender.class);
        given(settings.getProperty(DatabaseSettings.BACKEND)).willReturn(DataSourceType.MYSQL);
        given(dataSource.getType()).willReturn(DataSourceType.SQLITE);
        given(injector.retrieveAllOfType(Reloadable.class)).willReturn(new ArrayList<Reloadable>());
        given(injector.retrieveAllOfType(SettingsDependent.class)).willReturn(new ArrayList<SettingsDependent>());

        // when
        command.executeCommand(sender, Collections.<String>emptyList());

        // then
        verify(settings).reload();
        verify(injector, times(2)).retrieveAllOfType(any(Class.class));
        verify(sender).sendMessage(argThat(containsString("cannot change database type")));
    }

    private void verifyReloadingCalls(List<Reloadable> reloadables, List<SettingsDependent> dependents) {
        for (Reloadable reloadable : reloadables) {
            verify(reloadable).reload();
        }
        for (SettingsDependent dependent : dependents) {
            verify(dependent).reload(settings);
        }
    }
}
