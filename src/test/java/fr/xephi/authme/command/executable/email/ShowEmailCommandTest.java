package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.message.MessageKey;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link ShowEmailCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ShowEmailCommandTest {

    private static final String CURRENT_EMAIL = "my.email@example.com";
    private static final String USERNAME = "name";

    @InjectMocks
    private ShowEmailCommand command;

    @Mock
    private CommandService commandService;

    @Mock
    private PlayerCache playerCache;

    @Test
    public void shouldShowCurrentEmailMessage() {
        // given
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(USERNAME);
        given(playerCache.getAuth(USERNAME)).willReturn(newAuthWithEmail(CURRENT_EMAIL));

        // when
        command.executeCommand(sender, Collections.emptyList());

        // then
        verify(commandService).send(sender, MessageKey.EMAIL_SHOW, CURRENT_EMAIL);
    }

    @Test
    public void shouldReturnNoEmailMessage() {
        // given
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(USERNAME);
        given(playerCache.getAuth(USERNAME)).willReturn(newAuthWithNoEmail());

        // when
        command.executeCommand(sender, Collections.emptyList());

        // then
        verify(commandService).send(sender, MessageKey.SHOW_NO_EMAIL);
    }

    private static PlayerAuth newAuthWithEmail(String email) {
        return PlayerAuth.builder()
            .name(USERNAME)
            .email(email)
            .build();
    }

    private static PlayerAuth newAuthWithNoEmail() {
        return PlayerAuth.builder()
            .name(USERNAME)
            .build();
    }
}
