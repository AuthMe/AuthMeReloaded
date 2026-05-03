package fr.xephi.authme.bungee;

import net.md_5.bungee.api.CommandSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BungeeReloadCommandTest {

    @Mock
    private BungeeConfigManager configManager;

    @Mock
    private BungeeProxyBridge proxyBridge;

    @Mock
    private CommandSender commandSender;

    @Test
    void shouldReloadConfigAndProxyBridge() {
        BungeeProxyConfiguration configuration = new BungeeProxyConfiguration(
            Set.of("lobby"), false, true, Set.of("/login"), true, true,
            "Authentication required.", true, false, "", "", "");
        given(configManager.reload()).willReturn(configuration);

        BungeeReloadCommand command = new BungeeReloadCommand(configManager, proxyBridge);
        command.execute(commandSender, new String[0]);

        verify(proxyBridge).reload(configuration);
        verify(commandSender).sendMessage(any(net.md_5.bungee.api.chat.BaseComponent[].class));
    }
}
