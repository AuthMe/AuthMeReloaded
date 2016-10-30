package fr.xephi.authme;

import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import com.github.authme.configme.resource.PropertyResource;
import com.google.common.io.Files;
import fr.xephi.authme.api.NewAPI;
import fr.xephi.authme.command.CommandHandler;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.listener.BlockListener;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.process.login.ProcessSyncPlayerLogin;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.task.purge.PurgeService;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
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
import static fr.xephi.authme.settings.properties.AuthMeSettingsRetriever.buildConfigurationData;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Integration test verifying that all services can be initialized in {@link AuthMe}
 * with the {@link Injector}.
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
        Files.copy(TestHelper.getJarFile(TestHelper.PROJECT_ROOT + "config.test.yml"), settingsFile);

        // Mock / wire various Bukkit components
        given(server.getLogger()).willReturn(mock(Logger.class));
        ReflectionTestUtils.setField(Bukkit.class, null, "server", server);
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
        Settings settings =
            new Settings(dataFolder, mock(PropertyResource.class), alwaysFulfilled(), buildConfigurationData());

        Injector injector = new InjectorBuilder().addDefaultHandlers("fr.xephi.authme").create();
        injector.provide(DataFolder.class, dataFolder);
        injector.register(Server.class, server);
        injector.register(PluginManager.class, pluginManager);

        injector.register(AuthMe.class, authMe);
        injector.register(Settings.class, settings);
        injector.register(DataSource.class, mock(DataSource.class));
        injector.register(BukkitService.class, mock(BukkitService.class));

        // when
        authMe.instantiateServices(injector);
        authMe.registerEventListeners(injector);

        // then
        // Take a few samples and ensure that they are not null
        assertThat(injector.getIfAvailable(BlockListener.class), not(nullValue()));
        assertThat(injector.getIfAvailable(CommandHandler.class), not(nullValue()));
        assertThat(injector.getIfAvailable(Management.class), not(nullValue()));
        assertThat(injector.getIfAvailable(NewAPI.class), not(nullValue()));
        assertThat(injector.getIfAvailable(PasswordSecurity.class), not(nullValue()));
        assertThat(injector.getIfAvailable(PermissionsManager.class), not(nullValue()));
        assertThat(injector.getIfAvailable(ProcessSyncPlayerLogin.class), not(nullValue()));
        assertThat(injector.getIfAvailable(PurgeService.class), not(nullValue()));
    }

}
