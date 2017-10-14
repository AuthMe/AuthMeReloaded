package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.service.ValidationService.ValidationResult;
import org.bukkit.command.CommandSender;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link ChangePasswordAdminCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ChangePasswordAdminCommandTest {

    @InjectMocks
    private ChangePasswordAdminCommand command;

    @Mock
    private CommonService commonService;

    @Mock
    private ValidationService validationService;

    @Mock
    private Management management;

    @Test
    public void shouldForwardRequestToManagement() {
        // given
        String name = "theUser";
        String pass = "newPassword";
        given(validationService.validatePassword(pass, name)).willReturn(new ValidationResult());
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Arrays.asList(name, pass));

        // then
        verify(validationService).validatePassword(pass, name);
        verify(management).performPasswordChangeAsAdmin(sender, name, pass);
    }

    @Test
    public void shouldSendErrorToCommandSender() {
        // given
        String name = "theUser";
        String pass = "newPassword";
        given(validationService.validatePassword(pass, name)).willReturn(
            new ValidationResult(MessageKey.INVALID_PASSWORD_LENGTH, "7"));
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Arrays.asList(name, pass));

        // then
        verify(validationService).validatePassword(pass, name);
        verify(commonService).send(sender, MessageKey.INVALID_PASSWORD_LENGTH, "7");
        verifyZeroInteractions(management);
    }
}
