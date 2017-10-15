package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.service.CommonService;
import org.bukkit.command.CommandSender;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Date;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link LastLoginCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class LastLoginCommandTest {

    private static final long HOUR_IN_MSEC = 3600 * 1000;
    private static final long DAY_IN_MSEC = 24 * HOUR_IN_MSEC;

    @InjectMocks
    private LastLoginCommand command;

    @Mock
    private DataSource dataSource;

    @Mock
    private CommonService service;


    @Test
    public void shouldRejectNonExistentUser() {
        // given
        String player = "tester";
        given(dataSource.getAuth(player)).willReturn(null);
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList(player));

        // then
        verify(dataSource).getAuth(player);
        verify(service).send(sender, MessageKey.UNKNOWN_USER);
    }

    @Test
    public void shouldDisplayLastLoginOfUser() {
        // given
        String player = "SomePlayer";
        long lastLogin = System.currentTimeMillis() -
            (412 * DAY_IN_MSEC + 10 * HOUR_IN_MSEC - 9000);
        PlayerAuth auth = mock(PlayerAuth.class);
        given(auth.getLastLogin()).willReturn(lastLogin);
        given(auth.getLastIp()).willReturn("123.45.66.77");
        given(dataSource.getAuth(player)).willReturn(auth);
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList(player));

        // then
        verify(dataSource).getAuth(player);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sender, times(3)).sendMessage(captor.capture());
        String lastLoginString = new Date(lastLogin).toString();
        assertThat(captor.getAllValues().get(0),
            allOf(containsString(player), containsString(lastLoginString)));
        assertThat(captor.getAllValues().get(1), containsString("412 days 9 hours"));
        assertThat(captor.getAllValues().get(2), containsString("123.45.66.77"));
    }

    @Test
    public void shouldDisplayLastLoginOfCommandSender() {
        // given
        String name = "CommandSender";
        CommandSender sender = mock(CommandSender.class);
        given(sender.getName()).willReturn(name);

        long lastLogin = System.currentTimeMillis()
            - (412 * DAY_IN_MSEC + 10 * HOUR_IN_MSEC - 9000);
        PlayerAuth auth = mock(PlayerAuth.class);
        given(auth.getLastLogin()).willReturn(lastLogin);
        given(auth.getLastIp()).willReturn("123.45.66.77");
        given(dataSource.getAuth(name)).willReturn(auth);

        // when
        command.executeCommand(sender, Collections.emptyList());

        // then
        verify(dataSource).getAuth(name);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sender, times(3)).sendMessage(captor.capture());
        String lastLoginString = new Date(lastLogin).toString();
        assertThat(captor.getAllValues().get(0),
            allOf(containsString(name), containsString(lastLoginString)));
        assertThat(captor.getAllValues().get(1), containsString("412 days 9 hours"));
        assertThat(captor.getAllValues().get(2), containsString("123.45.66.77"));
    }

    @Test
    public void shouldHandleNullLastLoginDate() {
        // given
        String name = "player";
        PlayerAuth auth = PlayerAuth.builder()
            .name(name)
            .lastIp("123.45.67.89")
            .build();
        given(dataSource.getAuth(name)).willReturn(auth);
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList(name));

        // then
        verify(dataSource).getAuth(name);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sender, times(2)).sendMessage(captor.capture());
        assertThat(captor.getAllValues().get(0), allOf(containsString(name), containsString("never")));
        assertThat(captor.getAllValues().get(1), containsString("123.45.67.89"));
    }
}
