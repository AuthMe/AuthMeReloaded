package fr.xephi.authme.message;

import fr.xephi.authme.platform.BukkitCompatibilityAdapter;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

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
        BukkitCompatibilityAdapter adapter = mock(BukkitCompatibilityAdapter.class);
        given(adapter.getPlayerLocale(player)).willReturn(Optional.of("fr_fr"));
        Settings settings = mock(Settings.class);
        given(settings.getProperty(PluginSettings.PER_PLAYER_LOCALE)).willReturn(true);

        // when / then
        assertThat(PlayerLocaleResolver.resolveLanguage(settings, player, adapter), equalTo("fr"));
    }

    @Test
    void shouldResolveOverrideLocalePtBr() {
        // given pt_br is mapped to "br" via LOCALE_OVERRIDES
        Player player = mock(Player.class);
        BukkitCompatibilityAdapter adapter = mock(BukkitCompatibilityAdapter.class);
        given(adapter.getPlayerLocale(player)).willReturn(Optional.of("pt_br"));
        Settings settings = mock(Settings.class);
        given(settings.getProperty(PluginSettings.PER_PLAYER_LOCALE)).willReturn(true);

        // when / then
        assertThat(PlayerLocaleResolver.resolveLanguage(settings, player, adapter), equalTo("br"));
    }

    @Test
    void shouldReturnNullWhenAdapterReturnsEmpty() {
        // given adapter present but no locale available
        Player player = mock(Player.class);
        BukkitCompatibilityAdapter adapter = mock(BukkitCompatibilityAdapter.class);
        given(adapter.getPlayerLocale(player)).willReturn(Optional.empty());
        Settings settings = mock(Settings.class);
        given(settings.getProperty(PluginSettings.PER_PLAYER_LOCALE)).willReturn(true);

        // when / then
        assertThat(PlayerLocaleResolver.resolveLanguage(settings, player, adapter), nullValue());
    }

    @Test
    void shouldReturnNullWhenAdapterIsNull() {
        // given per-player locale enabled but no adapter capability
        Player player = mock(Player.class);
        Settings settings = mock(Settings.class);
        given(settings.getProperty(PluginSettings.PER_PLAYER_LOCALE)).willReturn(true);

        // when / then
        assertThat(PlayerLocaleResolver.resolveLanguage(settings, player, null), nullValue());
    }

    @Test
    void shouldReturnNullForPlayerWithPerPlayerLocaleDisabled() {
        // given
        Player player = mock(Player.class);
        BukkitCompatibilityAdapter adapter = mock(BukkitCompatibilityAdapter.class);
        Settings settings = mock(Settings.class);
        given(settings.getProperty(PluginSettings.PER_PLAYER_LOCALE)).willReturn(false);

        // when / then
        assertThat(PlayerLocaleResolver.resolveLanguage(settings, player, adapter), nullValue());
    }

    @Test
    void shouldReturnNullForNonPlayerSender() {
        // given
        CommandSender sender = mock(CommandSender.class);
        BukkitCompatibilityAdapter adapter = mock(BukkitCompatibilityAdapter.class);
        Settings settings = mock(Settings.class);
        given(settings.getProperty(PluginSettings.PER_PLAYER_LOCALE)).willReturn(true);

        // when / then
        assertThat(PlayerLocaleResolver.resolveLanguage(settings, sender, adapter), nullValue());
    }

    @Test
    void shouldReturnNullForNullSettings() {
        // given
        Player player = mock(Player.class);
        BukkitCompatibilityAdapter adapter = mock(BukkitCompatibilityAdapter.class);

        // when / then
        assertThat(PlayerLocaleResolver.resolveLanguage(null, player, adapter), nullValue());
    }
}
