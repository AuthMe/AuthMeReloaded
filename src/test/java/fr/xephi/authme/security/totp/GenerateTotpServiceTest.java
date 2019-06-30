package fr.xephi.authme.security.totp;

import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.security.totp.TotpAuthenticator.TotpGenerationResult;
import fr.xephi.authme.util.expiring.ExpiringMap;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link GenerateTotpService}.
 */
@RunWith(MockitoJUnitRunner.class)
public class GenerateTotpServiceTest {

    @InjectMocks
    private GenerateTotpService generateTotpService;

    @Mock
    private TotpAuthenticator totpAuthenticator;

    @Test
    public void shouldGenerateTotpKey() {
        // given
        TotpGenerationResult givenGenerationResult = new TotpGenerationResult("1234", "http://example.com/link/to/chart");
        Player player = mockPlayerWithName("Spencer");
        given(totpAuthenticator.generateTotpKey(player)).willReturn(givenGenerationResult);

        // when
        TotpGenerationResult result = generateTotpService.generateTotpKey(player);

        // then
        assertThat(result, equalTo(givenGenerationResult));
        assertThat(generateTotpService.getGeneratedTotpKey(player), equalTo(givenGenerationResult));
    }

    @Test
    public void shouldRemoveGeneratedTotpKey() {
        // given
        TotpGenerationResult givenGenerationResult = new TotpGenerationResult("1234", "http://example.com/link/to/chart");
        Player player = mockPlayerWithName("Hanna");
        given(totpAuthenticator.generateTotpKey(player)).willReturn(givenGenerationResult);
        generateTotpService.generateTotpKey(player);

        // when
        generateTotpService.removeGenerateTotpKey(player);

        // then
        assertThat(generateTotpService.getGeneratedTotpKey(player), nullValue());
    }

    @Test
    public void shouldCheckGeneratedTotpKey() {
        // given
        String generatedKey = "ASLO43KDF2J";
        TotpGenerationResult givenGenerationResult = new TotpGenerationResult(generatedKey, "url");
        Player player = mockPlayerWithName("Aria");
        given(totpAuthenticator.generateTotpKey(player)).willReturn(givenGenerationResult);
        generateTotpService.generateTotpKey(player);
        String validCode = "928374";
        given(totpAuthenticator.checkCode("Aria", generatedKey, validCode)).willReturn(true);

        // when
        boolean invalidCodeResult = generateTotpService.isTotpCodeCorrectForGeneratedTotpKey(player, "000000");
        boolean validCodeResult = generateTotpService.isTotpCodeCorrectForGeneratedTotpKey(player, validCode);
        boolean unknownPlayerResult = generateTotpService.isTotpCodeCorrectForGeneratedTotpKey(mockPlayerWithName("other"), "299874");

        // then
        assertThat(invalidCodeResult, equalTo(false));
        assertThat(validCodeResult, equalTo(true));
        assertThat(unknownPlayerResult, equalTo(false));
        verify(totpAuthenticator).checkCode("Aria", generatedKey, "000000");
        verify(totpAuthenticator).checkCode("Aria", generatedKey, validCode);
    }

    @Test
    public void shouldRemoveExpiredEntries() throws InterruptedException {
        // given
        TotpGenerationResult generationResult = new TotpGenerationResult("key", "url");
        ExpiringMap<String, TotpGenerationResult> generatedKeys =
            ReflectionTestUtils.getFieldValue(GenerateTotpService.class, generateTotpService, "totpKeys");
        generatedKeys.setExpiration(1, TimeUnit.MILLISECONDS);
        generatedKeys.put("ghost", generationResult);
        generatedKeys.setExpiration(5, TimeUnit.MINUTES);
        generatedKeys.put("ezra", generationResult);

        // when
        Thread.sleep(2L);
        generateTotpService.performCleanup();

        // then
        assertThat(generateTotpService.getGeneratedTotpKey(mockPlayerWithName("Ezra")), equalTo(generationResult));
        assertThat(generateTotpService.getGeneratedTotpKey(mockPlayerWithName("ghost")), nullValue());
    }

    private static Player mockPlayerWithName(String name) {
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        return player;
    }
}
