package fr.xephi.authme.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * Test for {@link UuidUtils}.
 */
class UuidUtilsTest {

    @Test
    void shouldParseUuidSafely() {
        // given
        UUID correctUuid = UUID.fromString("8e0a9aaa-5eda-42ef-8daf-e6c6359f607e");

        // when / then
        assertThat(UuidUtils.parseUuidSafely("8e0a9aaa-5eda-42ef-8daf-e6c6359f607e"), equalTo(correctUuid));

        assertThat(UuidUtils.parseUuidSafely(null), nullValue());
        assertThat(UuidUtils.parseUuidSafely(""), nullValue());
        assertThat(UuidUtils.parseUuidSafely("foo"), nullValue());
        assertThat(UuidUtils.parseUuidSafely("8e0a9aaa-5eda-42ef-InvalidEnding"), nullValue());
    }
}
