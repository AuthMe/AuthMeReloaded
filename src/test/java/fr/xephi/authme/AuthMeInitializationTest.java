package fr.xephi.authme;

import ch.jalu.configme.resource.PropertyReader;
import ch.jalu.configme.resource.PropertyResource;
import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import com.google.common.io.Files;
import fr.xephi.authme.api.v3.AuthMeApi;
import fr.xephi.authme.command.CommandHandler;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.listener.BlockListener;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.process.login.ProcessSyncPlayerLogin;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.bungeecord.BungeeReceiver;
import fr.xephi.authme.service.bungeecord.BungeeSender;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.task.purge.PurgeService;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.logging.Logger;

import static fr.xephi.authme.settings.properties.AuthMeSettingsRetriever.buildConfigurationData;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Integration test verifying that all services can be initialized in {@link AuthMe}
 * with the {@link Injector}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthMeInitializationTest {

    @Mock
    private Server server;

    @Mock
    private PluginManager pluginManager;

    private AuthMe authMe;
    private File dataFolder;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void initAuthMe() throws IOException {
        dataFolder = temporaryFolder.newFolder();
        File settingsFile = new File(dataFolder, "config.yml");
        given(server.getLogger()).willReturn(Logger.getAnonymousLogger());
        JavaPluginLoader pluginLoader = new JavaPluginLoader(server);
        Files.copy(TestHelper.getJarFile(TestHelper.PROJECT_ROOT + "config.test.yml"), settingsFile);

        // Mock / wire various Bukkit components
        ReflectionTestUtils.setField(Bukkit.class, null, "server", server);
        given(server.getPluginManager()).willReturn(pluginManager);

        // PluginDescriptionFile is final: need to create a sample one
        PluginDescriptionFile descriptionFile = new PluginDescriptionFile(
            "AuthMe", "N/A", AuthMe.class.getCanonicalName());

        // Initialize AuthMe
        authMe = new AuthMe(pluginLoader, descriptionFile, dataFolder, null);
    }

    @Test
    public void shouldInitializeAllServices() {
        // given
        PropertyReader reader = mock(PropertyReader.class);
        PropertyResource resource = mock(PropertyResource.class);
        given(resource.createReader()).willReturn(reader);
        Settings settings = new Settings(dataFolder, resource, null, buildConfigurationData());

        TestHelper.setupLogger();

        Injector injector = new InjectorBuilder()
            .addDefaultHandlers("fr.xephi.authme")
            .create();
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
        assertThat(injector.getIfAvailable(AuthMeApi.class), not(nullValue()));
        assertThat(injector.getIfAvailable(BlockListener.class), not(nullValue()));
        assertThat(injector.getIfAvailable(BungeeReceiver.class), not(nullValue()));
        assertThat(injector.getIfAvailable(BungeeSender.class), not(nullValue()));
        assertThat(injector.getIfAvailable(CommandHandler.class), not(nullValue()));
        assertThat(injector.getIfAvailable(Management.class), not(nullValue()));
        assertThat(injector.getIfAvailable(PasswordSecurity.class), not(nullValue()));
        assertThat(injector.getIfAvailable(PermissionsManager.class), not(nullValue()));
        assertThat(injector.getIfAvailable(ProcessSyncPlayerLogin.class), not(nullValue()));
        assertThat(injector.getIfAvailable(PurgeService.class), not(nullValue()));
    }

    @Test
    public void shouldHandlePrematureShutdownGracefully() {
        // given
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        given(server.getScheduler()).willReturn(scheduler);

        // Make sure ConsoleLogger has no logger reference since that may happen on unexpected stops
        ReflectionTestUtils.setField(ConsoleLogger.class, null, "logger", null);

        // when
        authMe.onDisable();

        // then - no exceptions
        verify(scheduler).getActiveWorkers(); // via TaskCloser
    }
}
