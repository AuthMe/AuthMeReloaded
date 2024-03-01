package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.PasswordRecoveryService;
import fr.xephi.authme.service.ValidationService;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link EmailSetPasswordCommand}.
 */
@ExtendWith(MockitoExtension.class)
class EmailSetPasswordCommandTest {

    @InjectMocks
    private EmailSetPasswordCommand command;

    @Mock
    private DataSource dataSource;

    @Mock
    private CommonService commonService;

    @Mock
    private PasswordRecoveryService recoveryService;

    @Mock
    private PasswordSecurity passwordSecurity;

    @Mock
    private ValidationService validationService;

    @BeforeAll
    static void setUpLogger() {
        TestHelper.setupLogger();
    }

    @Test
    void shouldChangePassword() {
        // given
        Player player = mock(Player.class);
        String name = "Jerry";
        given(player.getName()).willReturn(name);
        given(recoveryService.canChangePassword(player)).willReturn(true);
        HashedPassword hashedPassword = passwordSecurity.computeHash("abc123", name);
        given(passwordSecurity.computeHash("abc123", name)).willReturn(hashedPassword);
        given(validationService.validatePassword("abc123", name))
            .willReturn(new ValidationService.ValidationResult());

        // when
        command.runCommand(player, Collections.singletonList("abc123"));

        // then
        verify(validationService).validatePassword("abc123", name);
        verify(dataSource).updatePassword(name, hashedPassword);
        verify(recoveryService).removeFromSuccessfulRecovery(player);
        verify(commonService).send(player, MessageKey.PASSWORD_CHANGED_SUCCESS);
    }

    @Test
    void shouldRejectInvalidPassword() {
        // given
        Player player = mock(Player.class);
        String name = "Morgan";
        given(player.getName()).willReturn(name);
        String password = "newPW";
        given(validationService.validatePassword(password, name))
            .willReturn(new ValidationService.ValidationResult(MessageKey.INVALID_PASSWORD_LENGTH));
        given(recoveryService.canChangePassword(player)).willReturn(true);

        // when
        command.executeCommand(player, Collections.singletonList(password));

        // then
        verify(validationService).validatePassword(password, name);
        verify(commonService).send(player, MessageKey.INVALID_PASSWORD_LENGTH, new String[0]);
    }

    @Test
    void shouldDoNothingCantChangePass() {
        // given
        Player player = mock(Player.class);

        // when
        command.runCommand(player, Collections.singletonList("abc123"));

        // then
        verifyNoInteractions(validationService, dataSource);
        verify(commonService).send(player, MessageKey.CHANGE_PASSWORD_EXPIRED);
    }
}
