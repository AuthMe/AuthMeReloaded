package fr.xephi.authme.command.executable.totp;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.TempbanManager;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.limbo.LimboPlayer;
import fr.xephi.authme.data.limbo.LimboPlayerState;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.process.login.AsynchronousLogin;
import fr.xephi.authme.security.totp.TotpAuthenticator;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static fr.xephi.authme.service.BukkitServiceTestHelper.setBukkitServiceToScheduleSyncEntityTaskFromOptionallyAsyncTask;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link TotpCodeCommand}.
 */
@ExtendWith(MockitoExtension.class)
class TotpCodeCommandTest {

    private static final String IP = "203.0.113.5";

    @InjectMocks
    private TotpCodeCommand command;

    @Mock
    private LimboService limboService;
    @Mock
    private PlayerCache playerCache;
    @Mock
    private Messages messages;
    @Mock
    private TotpAuthenticator totpAuthenticator;
    @Mock
    private DataSource dataSource;
    @Mock
    private AsynchronousLogin asynchronousLogin;
    @Mock
    private TempbanManager tempbanManager;
    @Mock
    private BukkitService bukkitService;

    private Player player;
    private PlayerAuth auth;
    private LimboPlayer limbo;

    @BeforeAll
    static void setUpLogger() {
        TestHelper.setupLogger();
    }

    @BeforeEach
    void setUpTotpLimbo() {
        player = mock(Player.class);
        given(player.getName()).willReturn("Bobby");
        auth = PlayerAuth.builder().name("bobby").totpKey("secret").build();
        given(dataSource.getAuth("Bobby")).willReturn(auth);
        limbo = new LimboPlayer(null, false, Collections.emptyList(), false, 0.2f, 0.1f);
        limbo.setState(LimboPlayerState.TOTP_REQUIRED);
        given(limboService.getLimboPlayer("Bobby")).willReturn(limbo);
    }

    @Test
    void shouldCountWrongCodeAndKeepPlayerBeforeThirdAttempt() {
        // given
        TestHelper.mockIpAddressToPlayer(player, IP);
        given(totpAuthenticator.checkCode(auth, "000000")).willReturn(false);

        // when
        command.executeCommand(player, singletonList("000000"));
        command.executeCommand(player, singletonList("000000"));

        // then
        verify(tempbanManager, times(2)).increaseCount(IP, "bobby");
        verify(messages, times(2)).send(player, MessageKey.TWO_FACTOR_INVALID_CODE);
        verify(player, never()).kickPlayer(anyString());
        verify(asynchronousLogin, never()).performLogin(player, auth);
    }

    @Test
    void shouldKickPlayerAfterThirdWrongCode() {
        // given
        TestHelper.mockIpAddressToPlayer(player, IP);
        given(totpAuthenticator.checkCode(auth, "000000")).willReturn(false);
        given(messages.retrieveSingle(player, MessageKey.TWO_FACTOR_INVALID_CODE)).willReturn("Invalid code!");
        setBukkitServiceToScheduleSyncEntityTaskFromOptionallyAsyncTask(bukkitService);

        // when
        for (int i = 0; i < 3; ++i) {
            command.executeCommand(player, singletonList("000000"));
        }

        // then
        verify(player).kickPlayer("Invalid code!");
        verify(asynchronousLogin, never()).performLogin(player, auth);
    }

    @Test
    void shouldTempbanWhenThresholdIsReached() {
        // given
        TestHelper.mockIpAddressToPlayer(player, IP);
        given(totpAuthenticator.checkCode(auth, "000000")).willReturn(false);
        given(tempbanManager.shouldTempban(IP)).willReturn(true);

        // when
        command.executeCommand(player, singletonList("000000"));

        // then
        verify(tempbanManager).tempbanPlayer(player);
        verify(messages, never()).send(player, MessageKey.TWO_FACTOR_INVALID_CODE);
    }

    @Test
    void shouldLogInWithValidCode() {
        // given
        given(totpAuthenticator.checkCode(auth, "123456")).willReturn(true);

        // when
        command.executeCommand(player, singletonList("123456"));

        // then
        verify(asynchronousLogin).performLogin(player, auth);
        verify(tempbanManager, never()).increaseCount(anyString(), anyString());
    }
}
