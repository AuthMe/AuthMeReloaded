package fr.xephi.authme.command.executable.authme;

import ch.jalu.injector.factory.SingletonStore;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.DataSourceType;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.output.LogLevel;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.SettingsWarner;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Test for {@link ReloadCommand}.
 */
@ExtendWith(MockitoExtension.class)
class ReloadCommandTest {

    @InjectMocks
    private ReloadCommand command;

    @Mock
    private AuthMe authMe;

    @Mock
    private Settings settings;

    @Mock
    private DataSource dataSource;

    @Mock
    private CommonService commandService;

    @Mock
    private SettingsWarner settingsWarner;

    @Mock
    private SingletonStore<Reloadable> reloadableStore;

    @Mock
    private SingletonStore<SettingsDependent> settingsDependentStore;

    @BeforeAll
    static void setUpLogger() {
        TestHelper.setupLogger();
    }

    @BeforeEach
    void setDefaultSettings() {
        // Mock properties retrieved by ConsoleLogger
        given(settings.getProperty(PluginSettings.LOG_LEVEL)).willReturn(LogLevel.INFO);
        given(settings.getProperty(SecuritySettings.USE_LOGGING)).willReturn(false);
    }

    @Test
    void shouldReload() {
        // given
        CommandSender sender = mock(CommandSender.class);
        given(settings.getProperty(DatabaseSettings.BACKEND)).willReturn(DataSourceType.MYSQL);
        given(dataSource.getType()).willReturn(DataSourceType.MYSQL);
        List<Reloadable> reloadables = Arrays.asList(
            mock(Reloadable.class), mock(Reloadable.class), mock(Reloadable.class));
        List<SettingsDependent> dependents = Arrays.asList(
            mock(SettingsDependent.class), mock(SettingsDependent.class));
        given(reloadableStore.retrieveAllOfType()).willReturn(reloadables);
        given(settingsDependentStore.retrieveAllOfType()).willReturn(dependents);

        // when
        command.executeCommand(sender, Collections.emptyList());

        // then
        verify(settings).reload();
        verifyReloadingCalls(reloadables, dependents);
        verify(commandService).send(sender, MessageKey.CONFIG_RELOAD_SUCCESS);
        verify(settingsWarner).logWarningsForMisconfigurations();
    }

    @Test
    void shouldHandleReloadError() {
        // given
        CommandSender sender = mock(CommandSender.class);
        doThrow(IllegalStateException.class).when(reloadableStore).retrieveAllOfType();
        given(settings.getProperty(DatabaseSettings.BACKEND)).willReturn(DataSourceType.MYSQL);
        given(dataSource.getType()).willReturn(DataSourceType.MYSQL);

        // when
        command.executeCommand(sender, Collections.emptyList());

        // then
        verify(settings).reload();
        verify(reloadableStore).retrieveAllOfType();
        verify(sender).sendMessage(argThat(containsString("Error occurred")));
        verify(authMe).stopOrUnload();
    }

    @Test
    void shouldIssueWarningForChangedDataSourceSetting() {
        // given
        CommandSender sender = mock(CommandSender.class);
        given(settings.getProperty(DatabaseSettings.BACKEND)).willReturn(DataSourceType.MYSQL);
        given(dataSource.getType()).willReturn(DataSourceType.SQLITE);
        given(reloadableStore.retrieveAllOfType()).willReturn(Collections.emptyList());
        given(settingsDependentStore.retrieveAllOfType()).willReturn(Collections.emptyList());

        // when
        command.executeCommand(sender, Collections.emptyList());

        // then
        verify(settings).reload();
        verify(reloadableStore).retrieveAllOfType();
        verify(settingsDependentStore).retrieveAllOfType();
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
