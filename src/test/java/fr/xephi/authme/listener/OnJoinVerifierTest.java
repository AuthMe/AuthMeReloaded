package fr.xephi.authme.listener;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.service.AntiBotService;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.ProtectionSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerLoginEvent;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fr.xephi.authme.service.BukkitServiceTestHelper.returnGivenOnlinePlayers;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link OnJoinVerifier}.
 */
@RunWith(MockitoJUnitRunner.class)
public class OnJoinVerifierTest {

    @InjectMocks
    private OnJoinVerifier onJoinVerifier;

    @Mock
    private Settings settings;
    @Mock
    private DataSource dataSource;
    @Mock
    private Messages messages;
    @Mock
    private PermissionsManager permissionsManager;
    @Mock
    private AntiBotService antiBotService;
    @Mock
    private ValidationService validationService;
    @Mock
    private BukkitService bukkitService;
    @Mock
    private Server server;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @BeforeClass
    public static void setUpLogger() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldNotDoAnythingForNormalEvent() {
        // given
        PlayerLoginEvent event = mock(PlayerLoginEvent.class);
        given(event.getResult()).willReturn(PlayerLoginEvent.Result.ALLOWED);

        // when
        boolean result = onJoinVerifier.refusePlayerForFullServer(event);

        // then
        assertThat(result, equalTo(false));
        verify(event).getResult();
        verifyNoMoreInteractions(event);
        verifyZeroInteractions(bukkitService, dataSource, permissionsManager);
    }

    @Test
    public void shouldRefuseNonVipPlayerForFullServer() {
        // given
        Player player = mock(Player.class);
        PlayerLoginEvent event = new PlayerLoginEvent(player, "hostname", null);
        event.setResult(PlayerLoginEvent.Result.KICK_FULL);
        given(permissionsManager.hasPermission(player, PlayerStatePermission.IS_VIP)).willReturn(false);
        String serverFullMessage = "server is full";
        given(messages.retrieveSingle(player, MessageKey.KICK_FULL_SERVER)).willReturn(serverFullMessage);

        // when
        boolean result = onJoinVerifier.refusePlayerForFullServer(event);

        // then
        assertThat(result, equalTo(true));
        assertThat(event.getResult(), equalTo(PlayerLoginEvent.Result.KICK_FULL));
        assertThat(event.getKickMessage(), equalTo(serverFullMessage));
        verifyZeroInteractions(bukkitService, dataSource);
    }

    @Test
    public void shouldKickNonVipForJoiningVipPlayer() {
        // given
        Player player = mock(Player.class);
        PlayerLoginEvent event = new PlayerLoginEvent(player, "hostname", null);
        event.setResult(PlayerLoginEvent.Result.KICK_FULL);
        given(permissionsManager.hasPermission(player, PlayerStatePermission.IS_VIP)).willReturn(true);
        List<Player> onlinePlayers = Arrays.asList(mock(Player.class), mock(Player.class));
        given(permissionsManager.hasPermission(onlinePlayers.get(0), PlayerStatePermission.IS_VIP)).willReturn(true);
        given(permissionsManager.hasPermission(onlinePlayers.get(1), PlayerStatePermission.IS_VIP)).willReturn(false);
        returnGivenOnlinePlayers(bukkitService, onlinePlayers);
        given(server.getMaxPlayers()).willReturn(onlinePlayers.size());
        given(messages.retrieveSingle(player, MessageKey.KICK_FOR_VIP)).willReturn("kick for vip");

        // when
        boolean result = onJoinVerifier.refusePlayerForFullServer(event);

        // then
        assertThat(result, equalTo(false));
        assertThat(event.getResult(), equalTo(PlayerLoginEvent.Result.ALLOWED));
        // First player is VIP, so expect no interactions there and second player to have been kicked
        verifyZeroInteractions(onlinePlayers.get(0));
        verify(onlinePlayers.get(1)).kickPlayer("kick for vip");
    }

    @Test
    public void shouldKickVipPlayerIfNoPlayerCanBeKicked() {
        // given
        Player player = mock(Player.class);
        PlayerLoginEvent event = new PlayerLoginEvent(player, "hostname", null);
        event.setResult(PlayerLoginEvent.Result.KICK_FULL);
        given(permissionsManager.hasPermission(player, PlayerStatePermission.IS_VIP)).willReturn(true);
        List<Player> onlinePlayers = Collections.singletonList(mock(Player.class));
        given(permissionsManager.hasPermission(onlinePlayers.get(0), PlayerStatePermission.IS_VIP)).willReturn(true);
        returnGivenOnlinePlayers(bukkitService, onlinePlayers);
        given(server.getMaxPlayers()).willReturn(onlinePlayers.size());
        given(messages.retrieveSingle(player, MessageKey.KICK_FULL_SERVER)).willReturn("kick full server");

        // when
        boolean result = onJoinVerifier.refusePlayerForFullServer(event);

        // then
        assertThat(result, equalTo(true));
        assertThat(event.getResult(), equalTo(PlayerLoginEvent.Result.KICK_FULL));
        assertThat(event.getKickMessage(), equalTo("kick full server"));
        verifyZeroInteractions(onlinePlayers.get(0));
    }

    @Test
    public void shouldKickNonRegistered() throws FailedVerificationException {
        // given
        given(settings.getProperty(RestrictionSettings.KICK_NON_REGISTERED)).willReturn(true);

        // expect
        expectValidationExceptionWith(MessageKey.MUST_REGISTER_MESSAGE);

        // when
        onJoinVerifier.checkKickNonRegistered(false);
    }

    @Test
    public void shouldNotKickRegisteredPlayer() throws FailedVerificationException {
        // given / when / then
        onJoinVerifier.checkKickNonRegistered(true);
    }

    @Test
    public void shouldNotKickUnregisteredPlayer() throws FailedVerificationException {
        // given
        given(settings.getProperty(RestrictionSettings.KICK_NON_REGISTERED)).willReturn(false);

        // when
        onJoinVerifier.checkKickNonRegistered(false);
    }

    @Test
    public void shouldAllowValidName() throws FailedVerificationException {
        // given
        given(settings.getProperty(RestrictionSettings.MIN_NICKNAME_LENGTH)).willReturn(4);
        given(settings.getProperty(RestrictionSettings.MAX_NICKNAME_LENGTH)).willReturn(8);
        given(settings.getProperty(RestrictionSettings.ALLOWED_NICKNAME_CHARACTERS)).willReturn("[a-zA-Z0-9]+");
        onJoinVerifier.reload(); // @PostConstruct method

        // when
        onJoinVerifier.checkIsValidName("Bobby5");
    }

    @Test
    public void shouldRejectTooLongName() throws FailedVerificationException {
        // given
        given(settings.getProperty(RestrictionSettings.MAX_NICKNAME_LENGTH)).willReturn(8);
        given(settings.getProperty(RestrictionSettings.ALLOWED_NICKNAME_CHARACTERS)).willReturn("[a-zA-Z0-9]+");
        onJoinVerifier.reload(); // @PostConstruct method

        // expect
        expectValidationExceptionWith(MessageKey.INVALID_NAME_LENGTH);

        // when
        onJoinVerifier.checkIsValidName("longerthaneight");
    }

    @Test
    public void shouldRejectTooShortName() throws FailedVerificationException {
        // given
        given(settings.getProperty(RestrictionSettings.MIN_NICKNAME_LENGTH)).willReturn(4);
        given(settings.getProperty(RestrictionSettings.MAX_NICKNAME_LENGTH)).willReturn(8);
        given(settings.getProperty(RestrictionSettings.ALLOWED_NICKNAME_CHARACTERS)).willReturn("[a-zA-Z0-9]+");
        onJoinVerifier.reload(); // @PostConstruct method

        // expect
        expectValidationExceptionWith(MessageKey.INVALID_NAME_LENGTH);

        // when
        onJoinVerifier.checkIsValidName("abc");
    }

    @Test
    public void shouldRejectNameWithInvalidCharacters() throws FailedVerificationException {
        // given
        given(settings.getProperty(RestrictionSettings.MIN_NICKNAME_LENGTH)).willReturn(4);
        given(settings.getProperty(RestrictionSettings.MAX_NICKNAME_LENGTH)).willReturn(8);
        given(settings.getProperty(RestrictionSettings.ALLOWED_NICKNAME_CHARACTERS)).willReturn("[a-zA-Z0-9]+");
        onJoinVerifier.reload(); // @PostConstruct method

        // expect
        expectValidationExceptionWith(MessageKey.INVALID_NAME_CHARACTERS, "[a-zA-Z0-9]+");

        // when
        onJoinVerifier.checkIsValidName("Tester!");
    }

    @Test
    public void shouldAllowProperlyCasedName() throws FailedVerificationException {
        // given
        String name = "Bobby";
        PlayerAuth auth = PlayerAuth.builder().name("bobby").realName("Bobby").build();
        given(settings.getProperty(RegistrationSettings.PREVENT_OTHER_CASE)).willReturn(true);

        // when
        onJoinVerifier.checkNameCasing(name, auth);

        // then
        verifyZeroInteractions(dataSource);
    }

    @Test
    public void shouldRejectNameWithWrongCasing() throws FailedVerificationException {
        // given
        String name = "Tester";
        PlayerAuth auth = PlayerAuth.builder().name("tester").realName("testeR").build();
        given(settings.getProperty(RegistrationSettings.PREVENT_OTHER_CASE)).willReturn(true);

        // expect
        expectValidationExceptionWith(MessageKey.INVALID_NAME_CASE, "testeR", "Tester");

        // when / then
        onJoinVerifier.checkNameCasing(name, auth);
        verifyZeroInteractions(dataSource);
    }

    @Test
    public void shouldUpdateMissingRealName() throws FailedVerificationException {
        // given
        String name = "Authme";
        PlayerAuth auth = PlayerAuth.builder().name("authme").realName("").build();
        given(settings.getProperty(RegistrationSettings.PREVENT_OTHER_CASE)).willReturn(true);

        // when
        onJoinVerifier.checkNameCasing(name, auth);

        // then
        verify(dataSource).updateRealName("authme", "Authme");
    }

    @Test
    public void shouldUpdateDefaultRealName() throws FailedVerificationException {
        // given
        String name = "SOMEONE";
        PlayerAuth auth = PlayerAuth.builder().name("someone").realName("Player").build();
        given(settings.getProperty(RegistrationSettings.PREVENT_OTHER_CASE)).willReturn(true);

        // when
        onJoinVerifier.checkNameCasing(name, auth);

        // then
        verify(dataSource).updateRealName("someone", "SOMEONE");
    }

    @Test
    public void shouldAcceptCasingMismatchForDisabledSetting() throws FailedVerificationException {
        // given
        String name = "Test";
        PlayerAuth auth = PlayerAuth.builder().name("test").realName("TEST").build();
        given(settings.getProperty(RegistrationSettings.PREVENT_OTHER_CASE)).willReturn(false);

        // when
        onJoinVerifier.checkNameCasing(name, auth);

        // then
        verifyZeroInteractions(dataSource);
    }

    @Test
    public void shouldAcceptNameForUnregisteredAccount() throws FailedVerificationException {
        // given
        String name = "MyPlayer";
        PlayerAuth auth = null;

        // when
        onJoinVerifier.checkNameCasing(name, auth);

        // then
        verifyZeroInteractions(dataSource);
    }

    @Test
    public void shouldAcceptNameThatIsNotOnline() throws FailedVerificationException {
        // given
        String name = "bobby";
        given(settings.getProperty(RestrictionSettings.FORCE_SINGLE_SESSION)).willReturn(true);
        given(bukkitService.getPlayerExact("bobby")).willReturn(null);

        // when
        onJoinVerifier.checkSingleSession(name);

        // then
        verify(bukkitService).getPlayerExact(name);
    }

    @Test
    public void shouldRejectNameAlreadyOnline() throws FailedVerificationException {
        // given
        String name = "Charlie";

        Player onlinePlayer = mock(Player.class);

        given(bukkitService.getPlayerExact("Charlie")).willReturn(onlinePlayer);
        given(settings.getProperty(RestrictionSettings.FORCE_SINGLE_SESSION)).willReturn(true);

        // expect
        expectValidationExceptionWith(MessageKey.USERNAME_ALREADY_ONLINE_ERROR);

        // when / then
        onJoinVerifier.checkSingleSession(name);
    }

    @Test
    public void shouldAcceptAlreadyOnlineNameForDisabledSetting() throws FailedVerificationException {
        // given
        String name = "Felipe";
        given(settings.getProperty(RestrictionSettings.FORCE_SINGLE_SESSION)).willReturn(false);

        // when
        onJoinVerifier.checkSingleSession(name);

        // then
        verifyZeroInteractions(bukkitService);
    }

    @Test
    public void shouldAllowUser() throws FailedVerificationException {
        // given
        JoiningPlayer joiningPlayer = JoiningPlayer.fromName("Bobby");
        boolean isAuthAvailable = false;
        given(permissionsManager.hasPermission(joiningPlayer, PlayerStatePermission.BYPASS_ANTIBOT)).willReturn(false);
        given(antiBotService.shouldKick()).willReturn(false);

        // when
        onJoinVerifier.checkAntibot(joiningPlayer, isAuthAvailable);

        // then
        verify(permissionsManager).hasPermission(joiningPlayer, PlayerStatePermission.BYPASS_ANTIBOT);
        verify(antiBotService).shouldKick();
    }

    @Test
    public void shouldAllowUserWithAuth() throws FailedVerificationException {
        // given
        JoiningPlayer joiningPlayer = JoiningPlayer.fromName("Lacey");
        boolean isAuthAvailable = true;

        // when
        onJoinVerifier.checkAntibot(joiningPlayer, isAuthAvailable);

        // then
        verifyZeroInteractions(permissionsManager, antiBotService);
    }

    @Test
    public void shouldAllowUserWithBypassPermission() throws FailedVerificationException {
        // given
        JoiningPlayer joiningPlayer = JoiningPlayer.fromName("Steward");
        boolean isAuthAvailable = false;
        given(permissionsManager.hasPermission(joiningPlayer, PlayerStatePermission.BYPASS_ANTIBOT)).willReturn(true);

        // when
        onJoinVerifier.checkAntibot(joiningPlayer, isAuthAvailable);

        // then
        verify(permissionsManager).hasPermission(joiningPlayer, PlayerStatePermission.BYPASS_ANTIBOT);
        verifyZeroInteractions(antiBotService);
    }

    @Test
    public void shouldKickUserForFailedAntibotCheck() throws FailedVerificationException {
        // given
        JoiningPlayer joiningPlayer = JoiningPlayer.fromName("D3");
        boolean isAuthAvailable = false;
        given(permissionsManager.hasPermission(joiningPlayer, PlayerStatePermission.BYPASS_ANTIBOT)).willReturn(false);
        given(antiBotService.shouldKick()).willReturn(true);

        // when / then
        try {
            onJoinVerifier.checkAntibot(joiningPlayer, isAuthAvailable);
            fail("Expected exception to be thrown");
        } catch (FailedVerificationException e) {
            verify(permissionsManager).hasPermission(joiningPlayer, PlayerStatePermission.BYPASS_ANTIBOT);
            verify(antiBotService).shouldKick();
        }

    }

    /**
     * Tests various scenarios in which the country check should not take place.
     */
    @Test
    public void shouldNotCheckCountry() throws FailedVerificationException {
        JoiningPlayer joiningPlayer = JoiningPlayer.fromName("david");
        String ip = "127.0.0.1";

        // protection setting disabled
        given(settings.getProperty(ProtectionSettings.ENABLE_PROTECTION)).willReturn(false);
        given(settings.getProperty(ProtectionSettings.ENABLE_PROTECTION_REGISTERED)).willReturn(true);
        onJoinVerifier.checkPlayerCountry(joiningPlayer, ip, false);
        verifyZeroInteractions(validationService);

        // protection for registered players disabled
        given(settings.getProperty(ProtectionSettings.ENABLE_PROTECTION_REGISTERED)).willReturn(false);
        onJoinVerifier.checkPlayerCountry(joiningPlayer, ip, true);
        verifyZeroInteractions(validationService);
    }

    @Test
    public void shouldCheckAndAcceptUnregisteredPlayerCountry() throws FailedVerificationException {
        // given
        String ip = "192.168.0.1";
        JoiningPlayer joiningPlayer = JoiningPlayer.fromName("lucas");
        given(settings.getProperty(ProtectionSettings.ENABLE_PROTECTION)).willReturn(true);
        given(validationService.isCountryAdmitted(ip)).willReturn(true);

        // when
        onJoinVerifier.checkPlayerCountry(joiningPlayer, ip, false);

        // then
        verify(validationService).isCountryAdmitted(ip);
    }

    @Test
    public void shouldCheckAndAcceptRegisteredPlayerCountry() throws FailedVerificationException {
        // given
        String ip = "192.168.10.24";
        JoiningPlayer joiningPlayer = JoiningPlayer.fromName("gabriel");
        given(settings.getProperty(ProtectionSettings.ENABLE_PROTECTION)).willReturn(true);
        given(settings.getProperty(ProtectionSettings.ENABLE_PROTECTION_REGISTERED)).willReturn(true);
        given(validationService.isCountryAdmitted(ip)).willReturn(true);

        // when
        onJoinVerifier.checkPlayerCountry(joiningPlayer, ip, true);

        // then
        verify(validationService).isCountryAdmitted(ip);
    }

    @Test
    public void shouldThrowForBannedCountry() throws FailedVerificationException {
        // given
        String ip = "192.168.40.0";
        JoiningPlayer joiningPlayer = JoiningPlayer.fromName("bob");
        given(settings.getProperty(ProtectionSettings.ENABLE_PROTECTION)).willReturn(true);
        given(validationService.isCountryAdmitted(ip)).willReturn(false);

        // expect
        expectValidationExceptionWith(MessageKey.COUNTRY_BANNED_ERROR);

        // when
        onJoinVerifier.checkPlayerCountry(joiningPlayer, ip, false);
    }

    private void expectValidationExceptionWith(MessageKey messageKey, String... args) {
        expectedException.expect(exceptionWithData(messageKey, args));
    }

    private static Matcher<FailedVerificationException> exceptionWithData(final MessageKey messageKey,
                                                                          final String... args) {
        return new TypeSafeMatcher<FailedVerificationException>() {
            @Override
            protected boolean matchesSafely(FailedVerificationException item) {
                return messageKey.equals(item.getReason()) && Arrays.equals(args, item.getArgs());
            }

            @Override
            public void describeTo(Description description) {
                description.appendValue("VerificationFailedException: reason=" + messageKey + ";args="
                    + (args == null ? "null" : String.join(", ", args)));
            }
        };
    }

}
