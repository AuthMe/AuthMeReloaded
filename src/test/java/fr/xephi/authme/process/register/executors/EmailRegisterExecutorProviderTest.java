package fr.xephi.authme.process.register.executors;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.mail.EmailService;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.process.SyncProcessManager;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.settings.properties.EmailSettings;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static fr.xephi.authme.AuthMeMatchers.hasAuthBasicData;
import static fr.xephi.authme.AuthMeMatchers.hasAuthLocation;
import static fr.xephi.authme.AuthMeMatchers.stringWithLength;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link EmailRegisterExecutor}.
 */
@RunWith(MockitoJUnitRunner.class)
public class EmailRegisterExecutorProviderTest {

    @InjectMocks
    private EmailRegisterExecutor executor;

    @Mock
    private PermissionsManager permissionsManager;
    @Mock
    private DataSource dataSource;
    @Mock
    private CommonService commonService;
    @Mock
    private EmailService emailService;
    @Mock
    private SyncProcessManager syncProcessManager;
    @Mock
    private PasswordSecurity passwordSecurity;

    @Test
    public void shouldNotPassEmailValidation() {
        // given
        given(commonService.getProperty(EmailSettings.MAX_REG_PER_EMAIL)).willReturn(3);
        String email = "test@example.com";
        given(dataSource.countAuthsByEmail(email)).willReturn(4);
        Player player = mock(Player.class);
        EmailRegisterParams params = EmailRegisterParams.of(player, email);

        // when
        boolean result = executor.isRegistrationAdmitted(params);

        // then
        assertThat(result, equalTo(false));
        verify(dataSource).countAuthsByEmail(email);
        verify(permissionsManager).hasPermission(player, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS);
        verify(commonService).send(player, MessageKey.MAX_REGISTER_EXCEEDED, "3", "4", "@");
    }

    @Test
    public void shouldPassVerificationForPlayerWithPermission() {
        // given
        given(commonService.getProperty(EmailSettings.MAX_REG_PER_EMAIL)).willReturn(3);
        Player player = mock(Player.class);
        given(permissionsManager.hasPermission(player, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS)).willReturn(true);
        EmailRegisterParams params = EmailRegisterParams.of(player, "test@example.com");

        // when
        boolean result = executor.isRegistrationAdmitted(params);

        // then
        assertThat(result, equalTo(true));
        verify(permissionsManager).hasPermission(player, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS);
    }

    @Test
    public void shouldPassVerificationForPreviouslyUnregisteredIp() {
        // given
        given(commonService.getProperty(EmailSettings.MAX_REG_PER_EMAIL)).willReturn(1);
        String email = "test@example.com";
        given(dataSource.countAuthsByEmail(email)).willReturn(0);
        Player player = mock(Player.class);
        EmailRegisterParams params = EmailRegisterParams.of(player, "test@example.com");

        // when
        boolean result = executor.isRegistrationAdmitted(params);

        // then
        assertThat(result, equalTo(true));
        verify(permissionsManager).hasPermission(player, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS);
        verify(dataSource).countAuthsByEmail(email);
    }

    @Test
    @Ignore // TODO #792: last IP should be NULL + check registration info
    public void shouldCreatePlayerAuth() {
        // given
        given(commonService.getProperty(EmailSettings.RECOVERY_PASSWORD_LENGTH)).willReturn(12);
        given(passwordSecurity.computeHash(anyString(), anyString())).willAnswer(
            invocation -> new HashedPassword(invocation.getArgument(0)));
        Player player = mock(Player.class);
        TestHelper.mockPlayerIp(player, "123.45.67.89");
        given(player.getName()).willReturn("Veronica");
        World world = mock(World.class);
        given(world.getName()).willReturn("someWorld");
        given(player.getLocation()).willReturn(new Location(world, 48, 96, 144));
        EmailRegisterParams params = EmailRegisterParams.of(player, "test@example.com");

        // when
        PlayerAuth auth = executor.buildPlayerAuth(params);

        // then
        assertThat(auth, hasAuthBasicData("veronica", "Veronica", "test@example.com", "123.45.67.89"));
        assertThat(auth, hasAuthLocation(48, 96, 144, "someWorld", 0, 0));
        assertThat(auth.getPassword().getHash(), stringWithLength(12));
    }

    @Test
    public void shouldPerformActionAfterDataSourceSave() {
        // given
        given(emailService.sendPasswordMail(anyString(), anyString(), anyString())).willReturn(true);
        Player player = mock(Player.class);
        given(player.getName()).willReturn("Laleh");
        EmailRegisterParams params = EmailRegisterParams.of(player, "test@example.com");
        String password = "A892C#@";
        params.setPassword(password);

        // when
        executor.executePostPersistAction(params);

        // then
        verify(emailService).sendPasswordMail("Laleh", "test@example.com", password);
        verify(syncProcessManager).processSyncEmailRegister(player);
    }

    @Test
    public void shouldHandleEmailSendingFailure() {
        // given
        given(emailService.sendPasswordMail(anyString(), anyString(), anyString())).willReturn(false);
        Player player = mock(Player.class);
        given(player.getName()).willReturn("Laleh");
        EmailRegisterParams params = EmailRegisterParams.of(player, "test@example.com");
        String password = "A892C#@";
        params.setPassword(password);

        // when
        executor.executePostPersistAction(params);

        // then
        verify(emailService).sendPasswordMail("Laleh", "test@example.com", password);
        verify(commonService).send(player, MessageKey.EMAIL_SEND_FAILURE);
        verifyZeroInteractions(syncProcessManager);
    }

}
