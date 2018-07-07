package fr.xephi.authme.command.executable.authme.debug;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import org.bukkit.command.CommandSender;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Test for {@link PlayerAuthViewer}.
 */
@RunWith(MockitoJUnitRunner.class)
public class PlayerAuthViewerTest {

    @InjectMocks
    private PlayerAuthViewer authViewer;

    @Mock
    private DataSource dataSource;

    @Test
    public void shouldMakeExample() {
        // given
        CommandSender sender = mock(CommandSender.class);

        // when
        authViewer.execute(sender, Collections.emptyList());

        // then
        verify(sender).sendMessage(argThat(containsString("Example: /authme debug db Bobby")));
    }

    @Test
    public void shouldHandleMissingPlayer() {
        // given
        CommandSender sender = mock(CommandSender.class);

        // when
        authViewer.execute(sender, Collections.singletonList("bogus"));

        // then
        verify(dataSource).getAuth("bogus");
        verify(sender).sendMessage(argThat(containsString("No record exists for 'bogus'")));
    }

    @Test
    public void shouldDisplayAuthInfo() {
        // given
        CommandSender sender = mock(CommandSender.class);
        PlayerAuth auth = PlayerAuth.builder().name("george").realName("George")
            .password("abcdefghijkl", "mnopqrst")
            .lastIp("127.1.2.7").registrationDate(1111140000000L)
            .totpKey("SECRET1321")
            .build();
        given(dataSource.getAuth("George")).willReturn(auth);

        // when
        authViewer.execute(sender, Collections.singletonList("George"));

        // then
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(sender, atLeastOnce()).sendMessage(textCaptor.capture());
        assertThat(textCaptor.getAllValues(), hasItem(containsString("Player george / George")));
        assertThat(textCaptor.getAllValues(), hasItem(containsString("Registration: 2005-03-18T")));
        assertThat(textCaptor.getAllValues(), hasItem(containsString("Hash / salt (partial): 'abcdef...' / 'mnop...'")));
        assertThat(textCaptor.getAllValues(), hasItem(containsString("TOTP code (partial): 'SEC...'")));
    }

    @Test
    public void shouldHandleCornerCases() {
        // given
        CommandSender sender = mock(CommandSender.class);
        PlayerAuth auth = PlayerAuth.builder().name("tar")
            .password("abcd", null)
            .lastIp("127.1.2.7").registrationDate(0L)
            .build();
        given(dataSource.getAuth("Tar")).willReturn(auth);

        // when
        authViewer.execute(sender, Collections.singletonList("Tar"));

        // then
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(sender, atLeastOnce()).sendMessage(textCaptor.capture());
        assertThat(textCaptor.getAllValues(), hasItem(containsString("Player tar / Player")));
        assertThat(textCaptor.getAllValues(), hasItem(containsString("Registration: Not available (0)")));
        assertThat(textCaptor.getAllValues(), hasItem(containsString("Last login: Not available (null)")));
        assertThat(textCaptor.getAllValues(), hasItem(containsString("Hash / salt (partial): 'ab...' / ''")));
        assertThat(textCaptor.getAllValues(), hasItem(containsString("TOTP code (partial): ''")));
    }
}
