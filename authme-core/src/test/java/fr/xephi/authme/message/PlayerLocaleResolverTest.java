package fr.xephi.authme.message;

import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link PlayerLocaleResolver}.
 */
class PlayerLocaleResolverTest {

    @ParameterizedTest
    @CsvSource({
        "fr_fr, fr",
        "de_de, de",
        "en_us, en",
        "en_gb, en",
        "ru_ru, ru",
        "it_it, it",
        "nl_nl, nl",
        "pl_pl, pl",
        "ko_kr, ko",
        "ja_jp, ja",
        "tr_tr, tr",
        // special cases: locale without underscore
        "fr,    fr",
        "de,    de",
        // special overrides
        "pt_br, br",
        "zh_cn, zhcn",
        "zh_tw, zhtw",
        "zh_hk, zhhk",
    })
    void shouldMapLocaleToLanguageCode(String locale, String expected) {
        assertThat(PlayerLocaleResolver.toLanguageCode(locale), equalTo(expected));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void shouldReturnNullForBlankOrNullInput(String locale) {
        assertThat(PlayerLocaleResolver.toLanguageCode(locale), nullValue());
    }

    @Test
    void shouldResolveLanguageForPlayerWithPerPlayerLocaleEnabled() {
        // given
        Player player = mock(Player.class);
        given(player.getLocale()).willReturn("fr_fr");
        Settings settings = mock(Settings.class);
        given(settings.getProperty(PluginSettings.PER_PLAYER_LOCALE)).willReturn(true);

        // when / then
        assertThat(PlayerLocaleResolver.resolveLanguage(settings, player), equalTo("fr"));
    }

    @Test
    void shouldReturnNullForPlayerWithPerPlayerLocaleDisabled() {
        // given
        Player player = mock(Player.class);
        Settings settings = mock(Settings.class);
        given(settings.getProperty(PluginSettings.PER_PLAYER_LOCALE)).willReturn(false);

        // when / then
        assertThat(PlayerLocaleResolver.resolveLanguage(settings, player), nullValue());
    }

    @Test
    void shouldReturnNullForNonPlayerSender() {
        // given
        CommandSender sender = mock(CommandSender.class);
        Settings settings = mock(Settings.class);
        given(settings.getProperty(PluginSettings.PER_PLAYER_LOCALE)).willReturn(true);

        // when / then
        assertThat(PlayerLocaleResolver.resolveLanguage(settings, sender), nullValue());
    }

    @Test
    void shouldReturnNullForNullSettings() {
        // given
        Player player = mock(Player.class);

        // when / then
        assertThat(PlayerLocaleResolver.resolveLanguage(null, player), nullValue());
    }
}
