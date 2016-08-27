package fr.xephi.authme.cache.limbo;

import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.cache.backup.PlayerDataStorage;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.settings.properties.PluginSettings;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Test for {@link LimboCache}.
 */
@RunWith(MockitoJUnitRunner.class)
public class LimboCacheTest {

    @InjectMocks
    private LimboCache limboCache;

    @Mock
    private Settings settings;

    @Mock
    private PermissionsManager permissionsManager;

    @Mock
    private SpawnLoader spawnLoader;

    @Mock
    private PlayerDataStorage playerDataStorage;

    @Test
    public void shouldAddPlayerData() {
        // given
        Player player = mock(Player.class);
        String name = "Bobby";
        given(player.getName()).willReturn(name);
        Location location = mock(Location.class);
        given(spawnLoader.getPlayerLocationOrSpawn(player)).willReturn(location);
        given(player.isOp()).willReturn(true);
        float walkSpeed = 2.1f;
        given(player.getWalkSpeed()).willReturn(walkSpeed);
        given(player.getAllowFlight()).willReturn(true);
        float flySpeed = 3.0f;
        given(player.getFlySpeed()).willReturn(flySpeed);
        given(permissionsManager.hasGroupSupport()).willReturn(true);
        String group = "test-group";
        given(permissionsManager.getPrimaryGroup(player)).willReturn(group);
        given(playerDataStorage.hasData(player)).willReturn(false);

        // when
        limboCache.addPlayerData(player);

        // then
        PlayerData limboPlayer = limboCache.getPlayerData(name);
        assertThat(limboPlayer.getLocation(), equalTo(location));
        assertThat(limboPlayer.isOperator(), equalTo(true));
        assertThat(limboPlayer.getWalkSpeed(), equalTo(walkSpeed));
        assertThat(limboPlayer.isCanFly(), equalTo(true));
        assertThat(limboPlayer.getFlySpeed(), equalTo(flySpeed));
        assertThat(limboPlayer.getGroup(), equalTo(group));
    }

    @Test
    public void shouldGetPlayerDataFromDisk() {
        // given
        String name = "player01";
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        given(playerDataStorage.hasData(player)).willReturn(true);
        PlayerData playerData = mock(PlayerData.class);
        given(playerDataStorage.readData(player)).willReturn(playerData);
        float walkSpeed = 2.4f;
        given(playerData.getWalkSpeed()).willReturn(walkSpeed);
        given(playerData.isCanFly()).willReturn(true);
        float flySpeed = 1.0f;
        given(playerData.getFlySpeed()).willReturn(flySpeed);
        String group = "primary-group";
        given(playerData.getGroup()).willReturn(group);

        // when
        limboCache.addPlayerData(player);

        // then
        PlayerData result = limboCache.getPlayerData(name);
        assertThat(result.getWalkSpeed(), equalTo(walkSpeed));
        assertThat(result.isCanFly(), equalTo(true));
        assertThat(result.getFlySpeed(), equalTo(flySpeed));
        assertThat(result.getGroup(), equalTo(group));
    }

    @Test
    public void shouldRestorePlayerInfo() {
        // given
        String name = "Champ";
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        PlayerData playerData = mock(PlayerData.class);
        given(playerData.isOperator()).willReturn(true);
        float walkSpeed = 2.4f;
        given(playerData.getWalkSpeed()).willReturn(walkSpeed);
        given(playerData.isCanFly()).willReturn(true);
        float flySpeed = 1.0f;
        given(playerData.getFlySpeed()).willReturn(flySpeed);
        String group = "primary-group";
        given(playerData.getGroup()).willReturn(group);
        getCache().put(name.toLowerCase(), playerData);
        given(settings.getProperty(PluginSettings.ENABLE_PERMISSION_CHECK)).willReturn(true);
        given(permissionsManager.hasGroupSupport()).willReturn(true);

        // when
        limboCache.restoreData(player);

        // then
        verify(player).setOp(true);
        verify(player).setWalkSpeed(walkSpeed);
        verify(player).setAllowFlight(true);
        verify(player).setFlySpeed(flySpeed);
        verify(permissionsManager).setGroup(player, group);
        verify(playerData).clearTasks();
    }

    @Test
    public void shouldResetPlayerSpeed() {
        // given
        String name = "Champ";
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        PlayerData playerData = mock(PlayerData.class);
        given(playerData.isOperator()).willReturn(true);
        given(playerData.getWalkSpeed()).willReturn(0f);
        given(playerData.isCanFly()).willReturn(true);
        given(playerData.getFlySpeed()).willReturn(0f);
        String group = "primary-group";
        given(playerData.getGroup()).willReturn(group);
        getCache().put(name.toLowerCase(), playerData);
        given(settings.getProperty(PluginSettings.ENABLE_PERMISSION_CHECK)).willReturn(true);
        given(permissionsManager.hasGroupSupport()).willReturn(true);

        // when
        limboCache.restoreData(player);

        // then
        verify(player).setWalkSpeed(0.2f);
        verify(player).setFlySpeed(0.2f);
    }

    @Test
    public void shouldNotInteractWithPlayerIfNoDataAvailable() {
        // given
        String name = "player";
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);

        // when
        limboCache.restoreData(player);

        // then
        verify(player).getName();
        verifyNoMoreInteractions(player);
    }

    @Test
    public void shouldRemoveAndClearTasks() {
        // given
        PlayerData playerData = mock(PlayerData.class);
        String name = "abcdef";
        getCache().put(name, playerData);
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);

        // when
        limboCache.removeFromCache(player);

        // then
        assertThat(getCache(), anEmptyMap());
        verify(playerData).clearTasks();
    }

    @Test
    public void shouldDeleteFromCacheAndStorage() {
        // given
        PlayerData playerData = mock(PlayerData.class);
        String name = "SomeName";
        getCache().put(name.toLowerCase(), playerData);
        getCache().put("othername", mock(PlayerData.class));
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);

        // when
        limboCache.deletePlayerData(player);

        // then
        assertThat(getCache(), aMapWithSize(1));
        verify(playerData).clearTasks();
        verify(playerDataStorage).removeData(player);
    }

    @Test
    public void shouldReturnIfHasData() {
        // given
        String name = "tester";
        getCache().put(name, mock(PlayerData.class));

        // when / then
        assertThat(limboCache.hasPlayerData(name), equalTo(true));
        assertThat(limboCache.hasPlayerData("someone_else"), equalTo(false));
    }

    private Map<String, PlayerData> getCache() {
        return ReflectionTestUtils.getFieldValue(LimboCache.class, limboCache, "cache");
    }
}
