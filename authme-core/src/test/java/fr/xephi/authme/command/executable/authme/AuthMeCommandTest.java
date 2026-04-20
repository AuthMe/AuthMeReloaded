package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.ExecutableCommand;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link AuthMeCommand}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class AuthMeCommandTest {
    @Captor
    private ArgumentCaptor<String> messagesCaptor;

    @Test
    public void shouldDisplayInformation() {
        // given
        ExecutableCommand command = new AuthMeCommand();
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.emptyList());

        // then
        verify(sender, times(3)).sendMessage(messagesCaptor.capture());
        assertThat(messagesCaptor.getAllValues().get(1), containsString("/authme help"));
        assertThat(messagesCaptor.getAllValues().get(2), containsString("/authme about"));
    }
}

