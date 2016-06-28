package fr.xephi.authme;

import com.google.common.io.Files;
import fr.xephi.authme.api.NewAPI;
import fr.xephi.authme.command.CommandHandler;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.AuthMeServiceInitializer;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.listener.AuthMeBlockListener;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.process.login.ProcessSyncPlayerLogin;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.task.PurgeService;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import static fr.xephi.authme.settings.TestSettingsMigrationServices.alwaysFulfilled;
import static fr.xephi.authme.settings.properties.SettingsFieldRetriever.getAllPropertyFields;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Integration test verifying that all services can be initialized in {@link AuthMe}
 * with the {@link AuthMeServiceInitializer}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthMeInitializationTest {

    @Mock
    private PluginLoader pluginLoader;

    @Mock
    private Server server;

    @Mock
    private PluginManager pluginManager;

    private AuthMe authMe;
    private File dataFolder;
    private File settingsFile;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @BeforeClass
    public static void setUpLogger() {
        TestHelper.setupLogger();
    }

    @Before
    public void initAuthMe() throws IOException {
        dataFolder = temporaryFolder.newFolder();
        settingsFile = new File(dataFolder, "config.yml");
        Files.copy(TestHelper.getJarFile("/initialization/config.test.yml"), settingsFile);

        // Mock / wire various Bukkit components
        given(server.getLogger()).willReturn(mock(Logger.class));
        ReflectionTestUtils.setField(Bukkit.class, null, "server", server);
        given(server.getScheduler()).willReturn(mock(BukkitScheduler.class));
        given(server.getPluginManager()).willReturn(pluginManager);

        // PluginDescriptionFile is final: need to create a sample one
        PluginDescriptionFile descriptionFile = new PluginDescriptionFile(
            "AuthMe", "N/A", AuthMe.class.getCanonicalName());

        // Initialize AuthMe
        authMe = new AuthMe(pluginLoader, server, descriptionFile, dataFolder, null);
    }

    @Test
    public void shouldInitializeAllServices() {
        // given
        NewSetting settings = new NewSetting(settingsFile, dataFolder, getAllPropertyFields(), alwaysFulfilled());

        // TODO ljacqu 20160619: At some point setting the "plugin" field should not longer be necessary
        // We only require it right now because of usages of AuthMe#getInstance()
        ReflectionTestUtils.setField(AuthMe.class, null, "plugin", authMe);

        AuthMeServiceInitializer initializer = new AuthMeServiceInitializer("fr.xephi.authme");
        initializer.provide(DataFolder.class, dataFolder);
        initializer.register(Server.class, server);
        initializer.register(PluginManager.class, pluginManager);

        initializer.register(AuthMe.class, authMe);
        initializer.register(NewSetting.class, settings);
        initializer.register(DataSource.class, mock(DataSource.class));

        // when
        authMe.instantiateServices(initializer);
        authMe.registerEventListeners(initializer);

        // then
        // Take a few samples and ensure that they are not null
        assertThat(initializer.getIfAvailable(AuthMeBlockListener.class), not(nullValue()));
        assertThat(initializer.getIfAvailable(CommandHandler.class), not(nullValue()));
        assertThat(initializer.getIfAvailable(Management.class), not(nullValue()));
        assertThat(initializer.getIfAvailable(NewAPI.class), not(nullValue()));
        assertThat(initializer.getIfAvailable(PasswordSecurity.class), not(nullValue()));
        assertThat(initializer.getIfAvailable(PermissionsManager.class), not(nullValue()));
        assertThat(initializer.getIfAvailable(ProcessSyncPlayerLogin.class), not(nullValue()));
        assertThat(initializer.getIfAvailable(PurgeService.class), not(nullValue()));
    }

}
