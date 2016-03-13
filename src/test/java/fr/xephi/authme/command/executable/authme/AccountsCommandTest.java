package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import org.bukkit.command.CommandSender;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fr.xephi.authme.TestHelper.runInnerRunnable;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link AccountsCommand}.
 */
public class AccountsCommandTest {

    private AccountsCommand command;
    private CommandSender sender;
    private CommandService service;
    private DataSource dataSource;

    @Before
    public void setUpMocks() {
        command = new AccountsCommand();
        sender = mock(CommandSender.class);
        dataSource = mock(DataSource.class);
        service = mock(CommandService.class);
        when(service.getDataSource()).thenReturn(dataSource);
    }

    @Test
    public void shouldGetAccountsOfCurrentUser() {
        // given
        given(sender.getName()).willReturn("Tester");
        List<String> arguments = Collections.EMPTY_LIST;
        given(dataSource.getAuth("tester")).willReturn(authWithIp("123.45.67.89"));
        given(dataSource.getAllAuthsByIp("123.45.67.89")).willReturn(Arrays.asList("Toaster", "Pester"));

        // when
        command.executeCommand(sender, arguments, service);
        runInnerRunnable(service);

        // then
        String[] messages = getMessagesSentToSender(sender, 2);
        assertThat(messages[0], containsString("2 accounts"));
        assertThat(messages[1], containsString("Toaster, Pester"));
    }

    @Test
    public void shouldReturnUnknownUserForNullAuth() {
        // given
        List<String> arguments = Collections.singletonList("SomeUser");
        given(dataSource.getAuth("someuser")).willReturn(null);

        // when
        command.executeCommand(sender, arguments, service);
        runInnerRunnable(service);

        // then
        verify(service).send(sender, MessageKey.UNKNOWN_USER);
        verify(sender, never()).sendMessage(anyString());
    }

    @Test
    public void shouldReturnUnregisteredMessageForEmptyAuthList() {
        // given
        List<String> arguments = Collections.singletonList("SomeUser");
        given(dataSource.getAuth("someuser")).willReturn(mock(PlayerAuth.class));
        given(dataSource.getAllAuthsByIp(anyString())).willReturn(Collections.EMPTY_LIST);

        // when
        command.executeCommand(sender, arguments, service);
        runInnerRunnable(service);

        // then
        verify(service).send(sender, MessageKey.USER_NOT_REGISTERED);
        verify(sender, never()).sendMessage(anyString());
    }

    @Test
    public void shouldReturnSingleAccountMessage() {
        // given
        List<String> arguments = Collections.singletonList("SomeUser");
        given(dataSource.getAuth("someuser")).willReturn(authWithIp("56.78.90.123"));
        given(dataSource.getAllAuthsByIp("56.78.90.123")).willReturn(Collections.singletonList("SomeUser"));

        // when
        command.executeCommand(sender, arguments, service);
        runInnerRunnable(service);

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
        List<String> arguments = Collections.singletonList("123.45.67.89");
        given(dataSource.getAllAuthsByIp("123.45.67.89")).willReturn(Collections.EMPTY_LIST);

        // when
        command.executeCommand(sender, arguments, service);
        runInnerRunnable(service);

        // then
        String[] messages = getMessagesSentToSender(sender, 1);
        assertThat(messages[0], containsString("IP does not exist"));
    }

    @Test
    public void shouldReturnSingleAccountForIpQuery() {
        // given
        List<String> arguments = Collections.singletonList("24.24.48.48");
        given(dataSource.getAllAuthsByIp("24.24.48.48")).willReturn(Collections.singletonList("SomeUser"));

        // when
        command.executeCommand(sender, arguments, service);
        runInnerRunnable(service);

        // then
        String[] messages = getMessagesSentToSender(sender, 1);
        assertThat(messages[0], containsString("single account"));
    }

    @Test
    public void shouldReturnAccountListForIpQuery() {
        // given
        List<String> arguments = Collections.singletonList("98.76.41.122");
        given(dataSource.getAllAuthsByIp("98.76.41.122")).willReturn(Arrays.asList("Tester", "Lester", "Taster"));

        // when
        command.executeCommand(sender, arguments, service);
        runInnerRunnable(service);

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
            .ip(ip)
            .build();
    }
}
