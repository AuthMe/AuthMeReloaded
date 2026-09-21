package fr.xephi.authme.bungee.premium;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.PluginManager;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class PremiumVerificationManagersTest {

    @Test
    void shouldUseDisabledManagerWhenPacketEventsIsNotInstalled() {
        ProxyServer proxyServer = mock(ProxyServer.class);
        given(proxyServer.getPluginManager()).willReturn(mock(PluginManager.class));

        PremiumVerificationManager manager = PremiumVerificationManagers.create(
            proxyServer, mock(Logger.class), name -> true, name -> false, name -> { }, () -> true);

        assertNull(manager.getVerifiedPremiumUuid("alice"));
        manager.register();
        manager.refreshRegistration();
        manager.clearVerifiedPremium("alice");
        manager.shutdown();
    }
}
