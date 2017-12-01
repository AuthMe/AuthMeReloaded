package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import org.bukkit.command.CommandSender;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fr.xephi.authme.service.BukkitServiceTestHelper.setBukkitServiceToRunTaskAsynchronously;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link AccountsCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AccountsCommandTest {

    @InjectMocks
    private AccountsCommand command;
    @Mock
    private CommonService service;
    @Mock
    private DataSource dataSource;
    @Mock
    private BukkitService bukkitService;

    @Test
    public void shouldGetAccountsOfCurrentUser() {
        // given
        CommandSender sender = mock(CommandSender.class);
        given(sender.getName()).willReturn("Tester");
        List<String> arguments = Collections.emptyList();
        given(dataSource.getAuth("tester")).willReturn(authWithIp("123.45.67.89"));
        given(dataSource.getAllAuthsByIp("123.45.67.89")).willReturn(Arrays.asList("Toaster", "Pester"));
        setBukkitServiceToRunTaskAsynchronously(bukkitService);

        // when
        command.executeCommand(sender, arguments);

        // then
        String[] messages = getMessagesSentToSender(sender, 2);
        assertThat(messages[0], containsString("2 accounts"));
        assertThat(messages[1], containsString("Toaster, Pester"));
    }

    @Test
    public void shouldReturnUnknownUserForNullAuth() {
        // given
        CommandSender sender = mock(CommandSender.class);
        List<String> arguments = Collections.singletonList("SomeUser");
        given(dataSource.getAuth("someuser")).willReturn(null);
        setBukkitServiceToRunTaskAsynchronously(bukkitService);

        // when
        command.executeCommand(sender, arguments);

        // then
        verify(service).send(sender, MessageKey.UNKNOWN_USER);
        verify(sender, never()).sendMessage(anyString());
    }

    @Test
    public void shouldReturnUnregisteredMessageForEmptyAuthList() {
        // given
        CommandSender sender = mock(CommandSender.class);
        List<String> arguments = Collections.singletonList("SomeUser");
        PlayerAuth auth = authWithIp("144.56.77.88");
        given(dataSource.getAuth("someuser")).willReturn(auth);
        setBukkitServiceToRunTaskAsynchronously(bukkitService);

        // when
        command.executeCommand(sender, arguments);

        // then
        verify(service).send(sender, MessageKey.UNKNOWN_USER);
        verify(sender, never()).sendMessage(anyString());
    }

    @Test
    public void shouldReturnSingleAccountMessage() {
        // given
        CommandSender sender = mock(CommandSender.class);
        List<String> arguments = Collections.singletonList("SomeUser");
        given(dataSource.getAuth("someuser")).willReturn(authWithIp("56.78.90.123"));
        given(dataSource.getAllAuthsByIp("56.78.90.123")).willReturn(Collections.singletonList("SomeUser"));
        setBukkitServiceToRunTaskAsynchronously(bukkitService);

        // when
        command.executeCommand(sender, arguments);

        // then
        String[] messages = getMessagesSentToSender(sender, 1);
        assertThat(messages[0], containsString("single account"));
    }

    // -----
    // Query by IP
    // -----
    @Test
    public void shouldReturnIpUnknown() {
        // given
        CommandSender sender = mock(CommandSender.class);
        List<String> arguments = Collections.singletonList("123.45.67.89");
        given(dataSource.getAllAuthsByIp("123.45.67.89")).willReturn(Collections.emptyList());
        setBukkitServiceToRunTaskAsynchronously(bukkitService);

        // when
        command.executeCommand(sender, arguments);

        // then
        String[] messages = getMessagesSentToSender(sender, 1);
        assertThat(messages[0], containsString("IP does not exist"));
    }

    @Test
    public void shouldReturnSingleAccountForIpQuery() {
        // given
        CommandSender sender = mock(CommandSender.class);
        List<String> arguments = Collections.singletonList("24.24.48.48");
        given(dataSource.getAllAuthsByIp("24.24.48.48")).willReturn(Collections.singletonList("SomeUser"));
        setBukkitServiceToRunTaskAsynchronously(bukkitService);

        // when
        command.executeCommand(sender, arguments);

        // then
        String[] messages = getMessagesSentToSender(sender, 1);
        assertThat(messages[0], containsString("single account"));
    }

    @Test
    public void shouldReturnAccountListForIpQuery() {
        // given
        CommandSender sender = mock(CommandSender.class);
        List<String> arguments = Collections.singletonList("98.76.41.122");
        given(dataSource.getAllAuthsByIp("98.76.41.122")).willReturn(Arrays.asList("Tester", "Lester", "Taster"));
        setBukkitServiceToRunTaskAsynchronously(bukkitService);

        // when
        command.executeCommand(sender, arguments);

        // then
        String[] messages = getMessagesSentToSender(sender, 2);
        assertThat(messages[0], containsString("3 accounts"));
        assertThat(messages[1], containsString("Tester, Lester, Taster"));
    }

    private static String[] getMessagesSentToSender(CommandSender sender, int expectedCount) {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sender, times(expectedCount)).sendMessage(captor.capture());
        return captor.getAllValues().toArray(new String[expectedCount]);
    }

    private static PlayerAuth authWithIp(String ip) {
        return PlayerAuth.builder()
            .name("Test")
            .lastIp(ip)
            .build();
    }
}
