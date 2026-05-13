package fr.xephi.authme.bungee.premium;

import net.md_5.bungee.api.connection.PendingConnection;
import org.junit.jupiter.api.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class BungeePremiumOnlineModeHandlerTest {

    @Test
    void shouldEnableOnlineModeForPremiumPlayer() {
        PendingConnection connection = mock(PendingConnection.class);
        given(connection.getName()).willReturn("Alice");
        given(connection.isOnlineMode()).willReturn(false);

        BungeePremiumOnlineModeHandler handler = new BungeePremiumOnlineModeHandler("alice"::equals);
        handler.enableOnlineModeIfRequired(connection);

        verify(connection).setOnlineMode(true);
    }

    @Test
    void shouldIgnoreNonPremiumPlayer() {
        PendingConnection connection = mock(PendingConnection.class);
        given(connection.getName()).willReturn("Bob");

        BungeePremiumOnlineModeHandler handler = new BungeePremiumOnlineModeHandler("alice"::equals);
        handler.enableOnlineModeIfRequired(connection);

        verify(connection, never()).setOnlineMode(true);
    }
}
