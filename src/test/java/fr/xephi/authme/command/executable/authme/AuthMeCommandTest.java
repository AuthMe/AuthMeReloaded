package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.ExecutableCommand;
import org.bukkit.command.CommandSender;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link AuthMeCommand}.
 */
public class AuthMeCommandTest {

    @Test
    public void shouldDisplayInformation() {
        // given
        ExecutableCommand command = new AuthMeCommand();
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.emptyList());

        // then
        ArgumentCaptor<String> messagesCaptor = ArgumentCaptor.forClass(String.class);
        verify(sender, times(3)).sendMessage(messagesCaptor.capture());
        assertThat(messagesCaptor.getAllValues().get(1), containsString("/authme help"));
        assertThat(messagesCaptor.getAllValues().get(2), containsString("/authme about"));
    }
}
