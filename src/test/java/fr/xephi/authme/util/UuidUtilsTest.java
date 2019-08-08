package fr.xephi.authme.util;

import org.junit.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link UuidUtils}.
 */
public class UuidUtilsTest {

    @Test
    public void shouldParseUuidSafely() {
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
