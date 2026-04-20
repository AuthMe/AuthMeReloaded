package fr.xephi.authme.data;

import ch.jalu.datasourcecolumns.data.DataSourceValueImpl;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.mail.EmailService;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerPermission;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static fr.xephi.authme.service.BukkitServiceTestHelper.setBukkitServiceToRunTaskAsynchronously;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Test for {@link VerificationCodeManager}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class VerificationCodeManagerTest {

    @Mock
    private Settings settings;

    @Mock
    private DataSource dataSource;

    @Mock
    private EmailService emailService;

    @Mock
    private PermissionsManager permissionsManager;

    @Mock
    private BukkitService bukkitService;

    @Captor
    private ArgumentCaptor<Runnable> runnableCaptor;

    @BeforeEach
    public void setUpBasicBehavior() {
        given(emailService.hasAllInformation()).willReturn(true);
        given(settings.getProperty(SecuritySettings.VERIFICATION_CODE_EXPIRATION_MINUTES)).willReturn(1);
    }

    @Test
    public void shouldRequireVerification() {
        // given
        String name1 = "ILoveTests";
        Player player1 = mockPlayerWithName(name1);
        given(dataSource.getEmail(name1)).willReturn(DataSourceValueImpl.of("ilovetests@test.com"));
        given(permissionsManager.hasPermission(player1, PlayerPermission.VERIFICATION_CODE)).willReturn(true);
        String name2 = "StillLovingTests";
        Player player2 = mockPlayerWithName(name2);

        VerificationCodeManager codeManager = createCodeManager();
        codeManager.verify(name2);

        // when
        boolean test1 = codeManager.isVerificationRequired(player1);
        boolean test2 = codeManager.isVerificationRequired(player2);

        // then
        assertThat(test1, equalTo(true));
        assertThat(test2, equalTo(false));
        verify(dataSource, only()).getEmail(name1);
        verify(permissionsManager, only()).hasPermission(player1, PlayerPermission.VERIFICATION_CODE);
    }

    @Test
    public void shouldNotRequireVerificationIfEmailSettingsAreIncomplete() {
        // given
        given(emailService.hasAllInformation()).willReturn(false);
        VerificationCodeManager codeManager = createCodeManager();
        Player player = mock(Player.class);

        // when
        boolean result = codeManager.isVerificationRequired(player);

        // then
        assertThat(result, equalTo(false));
        verifyNoInteractions(permissionsManager, dataSource);
    }

    @Test
    public void shouldNotRequireVerificationForMissingPermission() {
        // given
        Player player = mockPlayerWithName("ILoveTests");
        given(permissionsManager.hasPermission(player, PlayerPermission.VERIFICATION_CODE)).willReturn(false);
        VerificationCodeManager codeManager = createCodeManager();

        // when
        boolean result = codeManager.isVerificationRequired(player);

        // then
        assertThat(result, equalTo(false));
        verify(permissionsManager).hasPermission(player, PlayerPermission.VERIFICATION_CODE);
        verifyNoInteractions(dataSource);
    }

    @Test
    public void shouldGenerateCode() {
        // given
        String player = "ILoveTests";
        String email = "ilovetests@test.com";
        given(dataSource.getEmail(player)).willReturn(DataSourceValueImpl.of(email));
        setBukkitServiceToRunTaskAsynchronously(bukkitService);
        VerificationCodeManager codeManager1 = createCodeManager();
        VerificationCodeManager codeManager2 = createCodeManager();
        codeManager2.codeExistOrGenerateNew(player);

        // when
        boolean test1 = codeManager1.hasCode(player);
        boolean test2 = codeManager2.hasCode(player);

        // then
        assertThat(test1, equalTo(false));
        assertThat(test2, equalTo(true));
    }

    @Test
    public void shouldRequireCode() {
        // given
        String player = "ILoveTests";
        String email = "ilovetests@test.com";
        given(dataSource.getEmail(player)).willReturn(DataSourceValueImpl.of(email));
        setBukkitServiceToRunTaskAsynchronously(bukkitService);
        VerificationCodeManager codeManager1 = createCodeManager();
        VerificationCodeManager codeManager2 = createCodeManager();
        codeManager2.codeExistOrGenerateNew(player);

        // when
        boolean test1 = codeManager1.isCodeRequired(player);
        boolean test2 = codeManager2.isCodeRequired(player);

        // then
        assertThat(test1, equalTo(false));
        assertThat(test2, equalTo(true));
    }

    @Test
    public void shouldVerifyCode() {
        // given
        String player = "ILoveTests";
        String code = "193458";
        String email = "ilovetests@test.com";
        given(dataSource.getEmail(player)).willReturn(DataSourceValueImpl.of(email));
        setBukkitServiceToRunTaskAsynchronously(bukkitService);
        VerificationCodeManager codeManager1 = createCodeManager();
        VerificationCodeManager codeManager2 = createCodeManager();
        codeManager1.codeExistOrGenerateNew(player);

        // when
        boolean test1 = codeManager1.checkCode(player, code);
        boolean test2 = codeManager2.checkCode(player, code);

        // then
        assertThat(test1, equalTo(false));
        assertThat(test2, equalTo(false));
    }

    @Test
    public void shouldLookupEmailOnlyInAsyncTask() {
        // given
        String player = "ILoveTests";
        String email = "ilovetests@test.com";
        VerificationCodeManager codeManager = createCodeManager();

        // when
        codeManager.codeExistOrGenerateNew(player);

        // then
        verify(bukkitService).runTaskAsynchronously(runnableCaptor.capture());
        verifyNoInteractions(dataSource);
        verify(emailService, only()).hasAllInformation();

        given(dataSource.getEmail(player)).willReturn(DataSourceValueImpl.of(email));
        runnableCaptor.getValue().run();

        assertThat(codeManager.hasCode(player), equalTo(true));
        verify(dataSource).getEmail(player);
        verify(emailService).sendVerificationMail(eq(player), eq(email), anyString());
    }

    @Test
    public void shouldNotScheduleDuplicateCodeGenerationWhilePending() {
        // given
        String player = "ILoveTests";
        VerificationCodeManager codeManager = createCodeManager();

        // when
        codeManager.codeExistOrGenerateNew(player);
        codeManager.codeExistOrGenerateNew(player);

        // then
        verify(bukkitService, times(1)).runTaskAsynchronously(any(Runnable.class));
        verifyNoInteractions(dataSource);
        verify(emailService, only()).hasAllInformation();
    }

    private VerificationCodeManager createCodeManager() {
        return new VerificationCodeManager(settings, dataSource, emailService, permissionsManager, bukkitService);
    }

    private static Player mockPlayerWithName(String name) {
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        return player;
    }
}


