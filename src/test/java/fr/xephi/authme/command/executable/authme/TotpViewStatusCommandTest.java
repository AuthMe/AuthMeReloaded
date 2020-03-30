package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Test for {@link TotpViewStatusCommand}.
 */
@ExtendWith(MockitoExtension.class)
class TotpViewStatusCommandTest {

    @InjectMocks
    private TotpViewStatusCommand command;

    @Mock
    private DataSource dataSource;

    @Mock
    private Messages messages;

    @BeforeAll
    static void initLogger() {
        TestHelper.setupLogger();
    }

    @Test
    void shouldHandleUnknownUser() {
        // given
        CommandSender sender = mock(CommandSender.class);
        given(dataSource.getAuth("user")).willReturn(null);

        // when
        command.executeCommand(sender, Collections.singletonList("user"));

        // then
        verify(messages).send(sender, MessageKey.UNKNOWN_USER);
        verify(dataSource, only()).getAuth("user");
    }

    @Test
    void shouldInformForUserWithoutTotp() {
        // given
        CommandSender sender = mock(CommandSender.class);
        PlayerAuth auth = PlayerAuth.builder()
            .name("billy")
            .totpKey(null)
            .build();
        given(dataSource.getAuth("Billy")).willReturn(auth);

        // when
        command.executeCommand(sender, Collections.singletonList("Billy"));

        // then
        verify(sender).sendMessage(argThat(containsString("'Billy' does NOT have two-factor auth enabled")));
    }

    @Test
    void shouldInformForUserWithTotpEnabled() {
        // given
        CommandSender sender = mock(CommandSender.class);
        PlayerAuth auth = PlayerAuth.builder()
            .name("billy")
            .totpKey("92841575")
            .build();
        given(dataSource.getAuth("Billy")).willReturn(auth);

        // when
        command.executeCommand(sender, Collections.singletonList("Billy"));

        // then
        verify(sender).sendMessage(argThat(containsString("'Billy' has enabled two-factor authentication")));
    }
}
