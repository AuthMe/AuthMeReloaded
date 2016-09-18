package fr.xephi.authme.api;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.hooks.PluginHooks;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.util.ValidationService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link NewAPI}.
 */
@RunWith(MockitoJUnitRunner.class)
public class NewAPITest {

    @InjectMocks
    private NewAPI api;

    @Mock
    private AuthMe authMe;
    @Mock
    private PluginHooks pluginHooks;
    @Mock
    private ValidationService validationService;
    @Mock
    private DataSource dataSource;
    @Mock
    private Management management;
    @Mock
    private PasswordSecurity passwordSecurity;
    @Mock
    private PlayerCache playerCache;

    @Test
    public void shouldReturnInstanceOrNull() {
        NewAPI result = NewAPI.getInstance();
        assertThat(result, sameInstance(api));

        ReflectionTestUtils.setField(NewAPI.class, null, "singleton", null);
        assertThat(NewAPI.getInstance(), nullValue());
    }

    @Test
    public void shouldReturnIfPlayerIsAuthenticated() {
        // given
        String name = "Bobby";
        Player player = mockPlayerWithName(name);
        given(playerCache.isAuthenticated(name)).willReturn(true);

        // when
        boolean result = api.isAuthenticated(player);

        // then
        verify(playerCache).isAuthenticated(name);
        assertThat(result, equalTo(true));
    }

    @Test
    public void shouldReturnIfPlayerIsNpc() {
        // given
        Player player = mock(Player.class);
        given(pluginHooks.isNpc(player)).willReturn(true);

        // when
        boolean result = api.isNPC(player);

        // then
        assertThat(result, equalTo(true));
    }

    @Test
    public void shouldReturnIfPlayerIsUnrestricted() {
        // given
        String name = "Tester";
        Player player = mockPlayerWithName(name);
        given(validationService.isUnrestricted(name)).willReturn(true);

        // when
        boolean result = api.isUnrestricted(player);

        // then
        verify(validationService).isUnrestricted(name);
        assertThat(result, equalTo(true));
    }

    @Test
    public void shouldGetLastLocation() {
        // given
        String name = "Gary";
        Player player = mockPlayerWithName(name);
        PlayerAuth auth = PlayerAuth.builder().name(name)
            .locWorld("world")
            .locX(12.4)
            .locY(24.6)
            .locZ(-438.2)
            .build();
        given(playerCache.getAuth(name)).willReturn(auth);
        Server server = mock(Server.class);
        ReflectionTestUtils.setField(Bukkit.class, null, "server", server);
        World world = mock(World.class);
        given(server.getWorld(auth.getWorld())).willReturn(world);

        // when
        Location result = api.getLastLocation(player);

        // then
        assertThat(result, not(nullValue()));
        assertThat(result.getX(), equalTo(auth.getQuitLocX()));
        assertThat(result.getY(), equalTo(auth.getQuitLocY()));
        assertThat(result.getZ(), equalTo(auth.getQuitLocZ()));
        assertThat(result.getWorld(), equalTo(world));
    }

    @Test
    public void shouldReturnNullForUnavailablePlayer() {
        // given
        String name = "Numan";
        Player player = mockPlayerWithName(name);
        given(playerCache.getAuth(name)).willReturn(null);

        // when
        Location result = api.getLastLocation(player);

        // then
        assertThat(result, nullValue());
    }

    @Test
    public void shouldCheckForRegisteredName() {
        // given
        String name = "toaster";
        given(dataSource.isAuthAvailable(name)).willReturn(true);

        // when
        boolean result = api.isRegistered(name);

        // then
        assertThat(result, equalTo(true));
    }

    @Test
    public void shouldCheckPassword() {
        // given
        String playerName = "Robert";
        String password = "someSecretPhrase2983";
        given(passwordSecurity.comparePassword(password, playerName)).willReturn(true);

        // when
        boolean result = api.checkPassword(playerName, password);

        // then
        verify(passwordSecurity).comparePassword(password, playerName);
        assertThat(result, equalTo(true));
    }

    @Test
    public void shouldReturnAuthNames() {
        // given
        String[] names = {"bobby", "peter", "elisabeth", "craig"};
        List<PlayerAuth> auths = Arrays.stream(names)
            .map(name -> PlayerAuth.builder().name(name).build())
            .collect(Collectors.toList());
        given(dataSource.getAllAuths()).willReturn(auths);

        // when
        List<String> result = api.getRegisteredNames();

        // then
        assertThat(result, contains(names));
    }

    @Test
    public void shouldReturnAuthRealNames() {
        // given
        String[] names = {"Bobby", "peter", "Elisabeth", "CRAIG"};
        List<PlayerAuth> auths = Arrays.stream(names)
            .map(name -> PlayerAuth.builder().name(name).realName(name).build())
            .collect(Collectors.toList());
        given(dataSource.getAllAuths()).willReturn(auths);

        // when
        List<String> result = api.getRegisteredRealNames();

        // then
        assertThat(result, contains(names));
    }

    @Test
    public void shouldUnregisterPlayer() {
        // given
        Player player = mock(Player.class);
        String name = "Donald";
        given(player.getName()).willReturn(name);

        // when
        api.forceUnregister(player);

        // then
        verify(management).performUnregisterByAdmin(null, name, player);
    }

    @Test
    public void shouldUnregisterPlayerByName() {
        // given
        Server server = mock(Server.class);
        ReflectionTestUtils.setField(Bukkit.class, null, "server", server);
        String name = "tristan";
        Player player = mock(Player.class);
        given(server.getPlayer(name)).willReturn(player);

        // when
        api.forceUnregister(name);

        // then
        verify(management).performUnregisterByAdmin(null, name, player);
    }

    private static Player mockPlayerWithName(String name) {
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        return player;
    }
}
