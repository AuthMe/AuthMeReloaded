package fr.xephi.authme.listener;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.service.PluginHookService;
import fr.xephi.authme.listener.protocollib.ProtocolLibService;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.SpawnLoader;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.PluginEvent;
import org.bukkit.plugin.Plugin;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link ServerListener}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerListenerTest {

    private static final String ESSENTIALS = "Essentials";
    private static final String ESSENTIALS_SPAWN = "EssentialsSpawn";
    private static final String CMI = "CMI";
    private static final String MULTIVERSE = "Multiverse-Core";
    private static final String PROTOCOL_LIB = "ProtocolLib";

    @InjectMocks
    private ServerListener serverListener;

    @Mock
    private PermissionsManager permissionsManager;

    @Mock
    private PluginHookService pluginHookService;

    @Mock
    private ProtocolLibService protocolLibService;

    @Mock
    private SpawnLoader spawnLoader;

    @BeforeClass
    public static void initLogger() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldForwardPluginNameOnEnable() {
        checkEnableHandling(ESSENTIALS,       () -> verify(pluginHookService).tryHookToEssentials());
        checkEnableHandling(ESSENTIALS_SPAWN, () -> verify(spawnLoader).loadEssentialsSpawn());
        checkEnableHandling(CMI,              () -> {
            verify(pluginHookService).tryHookToCmi();
            verify(spawnLoader).loadCmiSpawn();
        });
        checkEnableHandling(MULTIVERSE,       () -> verify(pluginHookService).tryHookToMultiverse());
        checkEnableHandling(PROTOCOL_LIB,     () -> verify(protocolLibService).setup());
        checkEnableHandling("UnknownPlugin",  () -> verifyZeroInteractions(pluginHookService, spawnLoader));
    }

    @Test
    public void shouldForwardPluginNameOnDisable() {
        checkDisableHandling(ESSENTIALS,       () -> verify(pluginHookService).unhookEssentials());
        checkDisableHandling(ESSENTIALS_SPAWN, () -> verify(spawnLoader).unloadEssentialsSpawn());
        checkDisableHandling(CMI,              () -> {
            verify(pluginHookService).unhookCmi();
            verify(spawnLoader).unloadCmiSpawn();
        });
        checkDisableHandling(MULTIVERSE,       () -> verify(pluginHookService).unhookMultiverse());
        checkDisableHandling(PROTOCOL_LIB,     () -> verify(protocolLibService).disable());
        checkDisableHandling("UnknownPlugin",  () -> verifyZeroInteractions(pluginHookService, spawnLoader));
    }

    @Test
    public void shouldHandlePluginWithNullName() {
        PluginEnableEvent enableEvent = mock(PluginEnableEvent.class);
        given(enableEvent.getPlugin()).willReturn(null);
        serverListener.onPluginEnable(enableEvent);
        verifyNoMoreInteractionsAndReset();

        PluginDisableEvent disableEvent = mock(PluginDisableEvent.class);
        given(disableEvent.getPlugin()).willReturn(null);
        serverListener.onPluginDisable(disableEvent);
        verifyNoMoreInteractionsAndReset();
    }

    private void checkEnableHandling(String pluginName, Runnable verifier) {
        PluginEnableEvent event = mockEventWithPluginName(PluginEnableEvent.class, pluginName);
        serverListener.onPluginEnable(event);
        verifier.run();
        verify(permissionsManager).onPluginEnable(pluginName);
        verifyNoMoreInteractionsAndReset();
    }

    private void checkDisableHandling(String pluginName, Runnable verifier) {
        PluginDisableEvent event = mockEventWithPluginName(PluginDisableEvent.class, pluginName);
        serverListener.onPluginDisable(event);
        verifier.run();
        verify(permissionsManager).onPluginDisable(pluginName);
        verifyNoMoreInteractionsAndReset();
    }

    private void verifyNoMoreInteractionsAndReset() {
        verifyNoMoreInteractions(permissionsManager, pluginHookService, protocolLibService, spawnLoader);
        reset(permissionsManager, pluginHookService, protocolLibService, spawnLoader);
    }

    private static  <T extends PluginEvent> T mockEventWithPluginName(Class<T> eventClass, String name) {
        T event = mock(eventClass);
        Plugin plugin = mock(Plugin.class);
        given(plugin.getName()).willReturn(name);
        given(event.getPlugin()).willReturn(plugin);
        return event;
    }
}
