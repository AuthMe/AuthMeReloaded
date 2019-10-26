package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.service.CommonService;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Test for {@link AddEmailCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AddEmailCommandTest {

    @InjectMocks
    private AddEmailCommand command;

    @Mock
    private CommonService commandService;

    @Mock
    private Management management;

    @Test
    public void shouldRejectNonPlayerSender() {
        // given
        CommandSender sender = mock(BlockCommandSender.class);

        // when
        command.executeCommand(sender, Collections.emptyList());

        // then
        verifyNoInteractions(management);
    }

    @Test
    public void shouldForwardData() {
        // given
        Player sender = mock(Player.class);
        String email = "mail@example";

        // when
        command.executeCommand(sender, Arrays.asList(email, email));

        // then
        verify(management).performAddEmail(sender, email);
    }

    @Test
    public void shouldFailForConfirmationMismatch() {
        // given
        Player sender = mock(Player.class);
        String email = "asdfasdf@example.com";

        // when
        command.executeCommand(sender, Arrays.asList(email, "wrongConf"));

        // then
        verifyNoInteractions(management);
        verify(commandService).send(sender, MessageKey.CONFIRM_EMAIL_MESSAGE);
    }

    @Test
    public void shouldDefineArgumentMismatchMessage() {
        // given / when / then
        assertThat(command.getArgumentsMismatchMessage(), equalTo(MessageKey.USAGE_ADD_EMAIL));
    }
}
