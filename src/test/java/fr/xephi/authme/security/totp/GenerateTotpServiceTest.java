package fr.xephi.authme.security.totp;

import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.security.totp.TotpAuthenticator.TotpGenerationResult;
import fr.xephi.authme.util.expiring.ExpiringMap;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link GenerateTotpService}.
 */
@ExtendWith(MockitoExtension.class)
class GenerateTotpServiceTest {

    @InjectMocks
    private GenerateTotpService generateTotpService;

    @Mock
    private TotpAuthenticator totpAuthenticator;

    @Test
    void shouldGenerateTotpKey() {
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
    void shouldRemoveGeneratedTotpKey() {
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
    void shouldCheckGeneratedTotpKey() {
        // given
        String generatedKey = "ASLO43KDF2J";
        TotpGenerationResult givenGenerationResult = new TotpGenerationResult(generatedKey, "url");
        Player player = mockPlayerWithName("Aria");
        given(totpAuthenticator.generateTotpKey(player)).willReturn(givenGenerationResult);
        generateTotpService.generateTotpKey(player);
        String validCode = "928374";
        given(totpAuthenticator.checkCode("Aria", generatedKey, validCode)).willReturn(true);
        given(totpAuthenticator.checkCode("Aria", generatedKey, "000000")).willReturn(false);

        // when
        boolean invalidCodeResult = generateTotpService.isTotpCodeCorrectForGeneratedTotpKey(player, "000000");
        boolean validCodeResult = generateTotpService.isTotpCodeCorrectForGeneratedTotpKey(player, validCode);
        boolean unknownPlayerResult = generateTotpService.isTotpCodeCorrectForGeneratedTotpKey(mockPlayerWithName("other"), "299874");

        // then
        assertThat(invalidCodeResult, equalTo(false));
        assertThat(validCodeResult, equalTo(true));
        assertThat(unknownPlayerResult, equalTo(false));
    }

    @Test
    void shouldRemoveExpiredEntries() throws InterruptedException {
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
