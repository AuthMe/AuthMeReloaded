package fr.xephi.authme.util;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.Settings;

import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Test for the {@link Utils} class.
 */
public class UtilsTest {

    private static AuthMe authMeMock;
    private PermissionsManager permissionsManagerMock;

    /**
     * The Utils class initializes its fields in a {@code static} block which is only executed once during the JUnit
     * tests, too. It is therefore important to initialize the mocks once with {@code @BeforeClass}. Initializing with
     * {@code @Before} as we usually do will create mocks that won't have any use in the Utils class.
     */
    @BeforeClass
    public static void setUpMocks() {
        WrapperMock wrapperMock = WrapperMock.createInstance();
        authMeMock = wrapperMock.getAuthMe();
    }

    @Before
    public void setIndirectMocks() {
        // Since the mocks aren't set up for each test case it is important to reset them when verifying whether or not
        // they have been called. We want to return null for permissions manager once so we initialize a mock for it
        // before every test -- this is OK because it is retrieved via authMeMock. It is just crucial that authMeMock
        // remain the same object.
        reset(authMeMock);

        permissionsManagerMock = mock(PermissionsManager.class);
        when(authMeMock.getPermissionsManager()).thenReturn(permissionsManagerMock);
    }

    @Test
    public void shouldNotAddToNormalGroupIfPermissionsAreDisabled() {
        // given
        Settings.isPermissionCheckEnabled = false;
        Player player = mock(Player.class);

        // when
        boolean result = Utils.addNormal(player, "test_group");

        // then
        assertThat(result, equalTo(false));
        verify(authMeMock, never()).getPermissionsManager();
    }

    @Test
    public void shouldNotAddToNormalGroupIfPermManagerIsNull() {
        // given
        Settings.isPermissionCheckEnabled = true;
        given(authMeMock.getPermissionsManager()).willReturn(null);
        Player player = mock(Player.class);

        // when
        boolean result = Utils.addNormal(player, "test_group");

        // then
        assertThat(result, equalTo(false));
        verify(authMeMock).getPermissionsManager();
    }

    @Test
    public void shouldRetrieveListOfOnlinePlayersFromReflectedMethod() {
        // given
        ReflectionTestUtils.setField(Utils.class, null, "getOnlinePlayersIsCollection", false);
        ReflectionTestUtils.setField(Utils.class, null, "getOnlinePlayers",
            ReflectionTestUtils.getMethod(UtilsTest.class, "onlinePlayersImpl"));

        // when
        Collection<? extends Player> players = Utils.getOnlinePlayers();

        // then
        assertThat(players, hasSize(2));
    }

    // Note: This method is used through reflections
    public static Player[] onlinePlayersImpl() {
        return new Player[]{
            mock(Player.class), mock(Player.class)
        };
    }

}
