package fr.xephi.authme.service;

import io.papermc.paper.connection.PlayerConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link PendingConnectionRegistry}.
 */
public class PendingConnectionRegistryTest {

    private static final long TTL = 30_000L;

    private PendingConnectionRegistry registry;

    @BeforeEach
    public void setUpRegistry() {
        registry = new PendingConnectionRegistry();
    }

    @Test
    public void shouldGrantClaimForFreeName() {
        PlayerConnection connection = newConnection(50_000);

        assertThat(registry.tryClaim("Bobby", connection, TTL), is(true));
        assertThat(registry.holdsClaim("Bobby", connection), is(true));
    }

    @Test
    public void shouldMatchClaimCaseInsensitively() {
        PlayerConnection connection = newConnection(50_000);
        registry.tryClaim("Bobby", connection, TTL);

        assertThat(registry.holdsClaim("bOBBy", connection), is(true));
        assertThat(registry.tryClaim("BOBBY", newConnection(50_001), TTL), is(false));
    }

    @Test
    public void shouldRefuseSecondLiveConnectionWithSameName() {
        PlayerConnection first = newConnection(50_000);
        PlayerConnection second = newConnection(50_001);
        registry.tryClaim("Bobby", first, TTL);

        assertThat(registry.tryClaim("Bobby", second, TTL), is(false));
        assertThat(registry.holdsClaim("Bobby", second), is(false));
        assertThat(registry.holdsClaim("Bobby", first), is(true));
    }

    @Test
    public void shouldReportNameClaimedByAnotherLiveConnection() {
        PlayerConnection holder = newConnection(50_000);
        PlayerConnection other = newConnection(50_001);
        registry.tryClaim("Bobby", holder, TTL);

        assertThat(registry.isClaimedByOtherConnection("Bobby", other), is(true));
        assertThat(registry.isClaimedByOtherConnection("Bobby", holder), is(false));
    }

    @Test
    public void shouldNotReportStaleOrMissingClaimAsHeldByAnotherConnection() {
        PlayerConnection other = newConnection(50_001);

        assertThat(registry.isClaimedByOtherConnection("Bobby", other), is(false));

        PlayerConnection gone = newConnection(50_000);
        registry.tryClaim("Bobby", gone, TTL);
        given(gone.isConnected()).willReturn(false);
        assertThat(registry.isClaimedByOtherConnection("Bobby", other), is(false));

        registry.tryClaim("Alice", newConnection(50_002), -1L);
        assertThat(registry.isClaimedByOtherConnection("Alice", other), is(false));
    }

    @Test
    public void shouldAllowSameConnectionToRenewItsClaim() {
        PlayerConnection connection = newConnection(50_000);
        registry.tryClaim("Bobby", connection, TTL);

        assertThat(registry.tryClaim("Bobby", connection, TTL), is(true));
    }

    @Test
    public void shouldTakeOverClaimOfDisconnectedConnection() {
        PlayerConnection gone = newConnection(50_000);
        registry.tryClaim("Bobby", gone, TTL);
        given(gone.isConnected()).willReturn(false);

        assertThat(registry.tryClaim("Bobby", newConnection(50_001), TTL), is(true));
    }

    @Test
    public void shouldTakeOverExpiredClaim() {
        registry.tryClaim("Bobby", newConnection(50_000), -1L);

        assertThat(registry.tryClaim("Bobby", newConnection(50_001), TTL), is(true));
    }

    @Test
    public void shouldNotConsiderExpiredClaimAsHeld() {
        PlayerConnection connection = newConnection(50_000);
        registry.tryClaim("Bobby", connection, -1L);

        assertThat(registry.holdsClaim("Bobby", connection), is(false));
    }

    @Test
    public void shouldReleaseClaim() {
        PlayerConnection connection = newConnection(50_000);
        registry.tryClaim("Bobby", connection, TTL);

        registry.release("Bobby");

        assertThat(registry.holdsClaim("Bobby", connection), is(false));
        assertThat(registry.tryClaim("Bobby", newConnection(50_001), TTL), is(true));
    }

    @Test
    public void shouldKeepClaimOfLiveConnectionWhenAnotherOneCloses() {
        PlayerConnection holder = newConnection(50_000);
        registry.tryClaim("Bobby", holder, TTL);

        // The refused duplicate connection closes and triggers the same close event
        registry.releaseIfStale("Bobby");

        assertThat(registry.holdsClaim("Bobby", holder), is(true));
        assertThat(registry.tryClaim("Bobby", newConnection(50_001), TTL), is(false));
    }

    @Test
    public void shouldReleaseClaimOfClosedConnection() {
        PlayerConnection holder = newConnection(50_000);
        registry.tryClaim("Bobby", holder, TTL);
        given(holder.isConnected()).willReturn(false);

        registry.releaseIfStale("Bobby");

        assertThat(registry.tryClaim("Bobby", newConnection(50_001), TTL), is(true));
    }

    @Test
    public void shouldHandleUnknownNameOnRelease() {
        registry.release("Bobby");
        registry.releaseIfStale("Bobby");

        assertThat(registry.holdsClaim("Bobby", newConnection(50_000)), is(false));
    }

    private static PlayerConnection newConnection(int port) {
        PlayerConnection connection = mock(PlayerConnection.class);
        given(connection.getAddress()).willReturn(new InetSocketAddress("127.0.0.1", port));
        given(connection.isConnected()).willReturn(true);
        return connection;
    }
}
