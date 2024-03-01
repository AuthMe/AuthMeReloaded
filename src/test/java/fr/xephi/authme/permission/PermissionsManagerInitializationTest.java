package fr.xephi.authme.permission;

import com.google.common.collect.ImmutableMap;
import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.permission.handlers.LuckPermsHandler;
import fr.xephi.authme.permission.handlers.PermissionHandler;
import fr.xephi.authme.permission.handlers.PermissionsExHandler;
import fr.xephi.authme.permission.handlers.VaultHandler;
import fr.xephi.authme.permission.handlers.ZPermissionsHandler;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.collect.Sets.newHashSet;
import static fr.xephi.authme.permission.PermissionsSystemType.LUCK_PERMS;
import static fr.xephi.authme.permission.PermissionsSystemType.PERMISSIONS_EX;
import static fr.xephi.authme.permission.PermissionsSystemType.VAULT;
import static fr.xephi.authme.permission.PermissionsSystemType.Z_PERMISSIONS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

/**
 * Tests the initialization of {@link PermissionHandler} in {@link PermissionsManager}.
 */
class PermissionsManagerInitializationTest {

    private Settings settings = mock(Settings.class);
    private ServicesManager servicesManager = mock(ServicesManager.class);
    private Server server = mock(Server.class);
    private PluginManager pluginManager = mock(PluginManager.class);
    private PermissionsManager permissionsManager = new PermissionsManager(server, pluginManager, settings);

    @BeforeAll
    static void setUpLogger() {
        TestHelper.setupLogger();
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(Bukkit.class, null, "server", server);
        given(server.getServicesManager()).willReturn(servicesManager);
    }

    @ParamTest
    void shouldInitializeHandler(PermissionsSystemType permissionsSystemType, Class<?> expectedHandlerType) {
        // given
        setUpForPermissionSystemTest(permissionsSystemType);
        given(settings.getProperty(PluginSettings.FORCE_VAULT_HOOK)).willReturn(false);
        Plugin plugin = mock(Plugin.class);
        given(plugin.isEnabled()).willReturn(true);
        given(pluginManager.getPlugin(permissionsSystemType.getPluginName())).willReturn(plugin);

        // when
        permissionsManager.setup();

        // then
        PermissionHandler handler = getHandlerFieldValue();
        assertThat(handler, instanceOf(expectedHandlerType));
        assertThat(handler.getPermissionSystem(), equalTo(permissionsSystemType));
    }

    @ParamTest
    void shouldInitializeToVaultIfSoConfigured(PermissionsSystemType permissionsSystemType, Class<?> expectedHandlerType) {
        // given
        setUpForVault();
        given(settings.getProperty(PluginSettings.FORCE_VAULT_HOOK)).willReturn(true);
        Plugin plugin = mock(Plugin.class);
        given(plugin.isEnabled()).willReturn(true);
        given(pluginManager.getPlugin(VAULT.getPluginName())).willReturn(plugin);

        // when
        permissionsManager.setup();

        // then
        PermissionHandler handler = getHandlerFieldValue();
        assertThat(handler, instanceOf(VaultHandler.class));
        verify(pluginManager, only()).getPlugin(VAULT.getPluginName());
    }

    @ParamTest
    void shouldNotHookIntoDisabledPlugin(PermissionsSystemType permissionsSystemType, Class<?> expectedHandlerType) {
        // given
        given(settings.getProperty(PluginSettings.FORCE_VAULT_HOOK)).willReturn(false);
        Plugin plugin = mock(Plugin.class);
        given(plugin.isEnabled()).willReturn(false);
        given(pluginManager.getPlugin(permissionsSystemType.getPluginName())).willReturn(plugin);

        // when
        permissionsManager.setup();

        // then
        assertThat(getHandlerFieldValue(), nullValue());
    }

    @ParamTest
    void shouldCatchInitializationException(PermissionsSystemType permissionsSystemType, Class<?> expectedHandlerType) {
        // given
        given(settings.getProperty(PluginSettings.FORCE_VAULT_HOOK)).willReturn(false);
        Plugin plugin = mock(Plugin.class);
        // Typically we'd expect a PermissionHandler exception further down the line but we can test it easily like this
        given(plugin.isEnabled()).willThrow(new IllegalStateException("Some exception occurred"));
        given(pluginManager.getPlugin(permissionsSystemType.getPluginName())).willReturn(plugin);

        // when
        permissionsManager.setup();

        // then
        assertThat(getHandlerFieldValue(), nullValue());
    }

    static Collection<Object[]> createParameters() {
        Map<PermissionsSystemType, Class<?>> handlersByPermissionSystemType = ImmutableMap.of(
            LUCK_PERMS, LuckPermsHandler.class,
            PERMISSIONS_EX, PermissionsExHandler.class,
            Z_PERMISSIONS, ZPermissionsHandler.class,
            VAULT, VaultHandler.class);

        // Verify that all handlers are present -> reminder to add any new entry here as well
        if (!handlersByPermissionSystemType.keySet().equals(newHashSet(PermissionsSystemType.values()))) {
            throw new IllegalStateException("Test is not set up with all "
                + PermissionsSystemType.class.getSimpleName() + " entries");
        }

        // Wrap the above map in a Collection<Object[]> to satisfy JUnit
        return handlersByPermissionSystemType.entrySet().stream()
            .map(e -> new Object[]{ e.getKey(), e.getValue() })
            .collect(Collectors.toList());
    }

    private void setUpForPermissionSystemTest(PermissionsSystemType permissionsSystemType) {
        if (permissionsSystemType == LUCK_PERMS) {
            LuckPerms api = mock(LuckPerms.class);
            ReflectionTestUtils.setField(LuckPermsProvider.class, null, "instance", api);
        } else if (permissionsSystemType == PERMISSIONS_EX) {
            assumeTrue(false,
                "PermissionsEx instance cannot be mocked because of missing dependencies -- skipping");
        } else if (permissionsSystemType == Z_PERMISSIONS) {
            ZPermissionsService zPermissionsService = mock(ZPermissionsService.class);
            given(servicesManager.load(ZPermissionsService.class)).willReturn(zPermissionsService);
        } else if (permissionsSystemType == VAULT) {
            setUpForVault();
        } else {
            throw new IllegalStateException("Unhandled permission systems type: " + permissionsSystemType);
        }
    }

    private void setUpForVault() {
        RegisteredServiceProvider<Permission> registeredServiceProvider = mock(RegisteredServiceProvider.class);
        given(servicesManager.getRegistration(Permission.class)).willReturn(registeredServiceProvider);
        Permission permission = mock(Permission.class);
        given(registeredServiceProvider.getProvider()).willReturn(permission);
    }

    private PermissionHandler getHandlerFieldValue() {
        return ReflectionTestUtils.getFieldValue(PermissionsManager.class, permissionsManager, "handler");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("createParameters")
    @Retention(RetentionPolicy.RUNTIME)
    @interface ParamTest {
    }
}
