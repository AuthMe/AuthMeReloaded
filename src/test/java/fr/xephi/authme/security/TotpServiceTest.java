package fr.xephi.authme.security;

import fr.xephi.authme.security.TotpService.TotpGenerationResult;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static fr.xephi.authme.AuthMeMatchers.stringWithLength;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link TotpService}.
 */
@RunWith(MockitoJUnitRunner.class)
public class TotpServiceTest {

    @InjectMocks
    private TotpService totpService;

    @Mock
    private BukkitService bukkitService;

    @Test
    public void shouldGenerateTotpKey() {
        // given
        Player player = mock(Player.class);
        given(player.getName()).willReturn("Bobby");
        given(bukkitService.getIp()).willReturn("127.48.44.4");

        // when
        TotpGenerationResult key1 = totpService.generateTotpKey(player);
        TotpGenerationResult key2 = totpService.generateTotpKey(player);

        // then
        assertThat(key1.getTotpKey(), stringWithLength(16));
        assertThat(key2.getTotpKey(), stringWithLength(16));
        assertThat(key1.getAuthenticatorQrCodeUrl(), startsWith("https://chart.googleapis.com/chart?chs=200x200"));
        assertThat(key2.getAuthenticatorQrCodeUrl(), startsWith("https://chart.googleapis.com/chart?chs=200x200"));
        assertThat(key1.getTotpKey(), not(equalTo(key2.getTotpKey())));
    }

}
