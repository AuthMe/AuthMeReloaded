package fr.xephi.authme.message;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

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
}
