package fr.xephi.authme.service;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.service.bungeecord.BungeeSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
class ProxyLoginRequestValidatorTest {

    @InjectMocks
    private ProxyLoginRequestValidator validator;

    @Mock
    private DataSource dataSource;

    @Mock
    private fr.xephi.authme.data.auth.PlayerCache playerCache;

    @Mock
    private PendingPremiumCache pendingPremiumCache;

    @Mock
    private PremiumService premiumService;

    @Mock
    private BungeeSender bungeeSender;

    @Mock
    private Messages messages;

    @Mock
    private Player player;

    @Test
    void shouldAcceptStoredPremiumUuidFromProxy() {
        UUID premiumUuid = UUID.randomUUID();
        PlayerAuth auth = PlayerAuth.builder().name("bobby").premiumUuid(premiumUuid).build();
        given(player.getName()).willReturn("Bobby");
        given(playerCache.getAuth("Bobby")).willReturn(auth);

        assertTrue(validator.validate(player, premiumUuid));
        verify(premiumService, never()).finalizePendingPremium(player, premiumUuid);
    }

    @Test
    void shouldRejectStoredPremiumUuidMismatch() {
        UUID storedUuid = UUID.randomUUID();
        UUID forwardedUuid = UUID.randomUUID();
        PlayerAuth auth = PlayerAuth.builder().name("bobby").premiumUuid(storedUuid).build();
        given(player.getName()).willReturn("Bobby");
        given(playerCache.getAuth("Bobby")).willReturn(auth);

        assertFalse(validator.validate(player, forwardedUuid));
        verify(premiumService, never()).finalizePendingPremium(player, forwardedUuid);
    }

    @Test
    void shouldFinalizeMatchingPendingPremiumUuid() {
        UUID pendingUuid = UUID.randomUUID();
        PlayerAuth auth = PlayerAuth.builder().name("bobby").build();
        given(player.getName()).willReturn("Bobby");
        given(playerCache.getAuth("Bobby")).willReturn(auth);
        given(pendingPremiumCache.removePending("Bobby")).willReturn(pendingUuid);

        assertTrue(validator.validate(player, pendingUuid));
        verify(premiumService).finalizePendingPremium(player, pendingUuid);
    }

    @Test
    void shouldRejectPendingPremiumUuidMismatchAndNotifyPlayer() {
        UUID pendingUuid = UUID.randomUUID();
        UUID forwardedUuid = UUID.randomUUID();
        PlayerAuth auth = PlayerAuth.builder().name("bobby").build();
        given(player.getName()).willReturn("Bobby");
        given(playerCache.getAuth("Bobby")).willReturn(auth);
        given(pendingPremiumCache.removePending("Bobby")).willReturn(pendingUuid);

        assertFalse(validator.validate(player, forwardedUuid));
        verify(bungeeSender).sendPremiumUnset("Bobby");
        verify(messages).send(player, MessageKey.PREMIUM_PENDING_FAIL);
        verify(premiumService, never()).finalizePendingPremium(player, forwardedUuid);
    }
}
