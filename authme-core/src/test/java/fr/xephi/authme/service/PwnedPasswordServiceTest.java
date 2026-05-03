package fr.xephi.authme.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.OptionalLong;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Test for {@link PwnedPasswordService}.
 */
class PwnedPasswordServiceTest {

    @Test
    void shouldReturnPwnedCountForMatchingPasswordHash() {
        // given
        TestPwnedPasswordService service = new TestPwnedPasswordService(
            "003D68EB55068C33ACE09247EE4C639306B:1\n"
                + "1E4C9B93F3F0682250B6CF8331B7EE68FD8:12345\n"
                + "FFFFF0AC487871FEEC1891C490136E006E2:0");

        // when
        OptionalLong count = service.getPwnedCount("password");

        // then
        assertThat(count.isPresent(), equalTo(true));
        assertThat(count.getAsLong(), equalTo(12345L));
        assertThat(service.requestedHashPrefix, equalTo("5BAA6"));
    }

    @Test
    void shouldReturnZeroWhenPasswordHashIsAbsent() {
        // given
        TestPwnedPasswordService service = new TestPwnedPasswordService(
            "003D68EB55068C33ACE09247EE4C639306B:1\n"
                + "FFFFF0AC487871FEEC1891C490136E006E2:0");

        // when
        OptionalLong count = service.getPwnedCount("password");

        // then
        assertThat(count.isPresent(), equalTo(true));
        assertThat(count.getAsLong(), equalTo(0L));
    }

    @Test
    void shouldReturnEmptyWhenRangeRequestFails() {
        // given
        TestPwnedPasswordService service = new TestPwnedPasswordService(new IOException("boom"));

        // when
        OptionalLong count = service.getPwnedCount("password");

        // then
        assertThat(count.isPresent(), equalTo(false));
    }

    private static final class TestPwnedPasswordService extends PwnedPasswordService {
        private final String response;
        private final IOException exception;
        private String requestedHashPrefix;

        private TestPwnedPasswordService(String response) {
            this.response = response;
            this.exception = null;
        }

        private TestPwnedPasswordService(IOException exception) {
            this.response = null;
            this.exception = exception;
        }

        @Override
        protected String requestHashRange(String hashPrefix) throws IOException {
            requestedHashPrefix = hashPrefix;
            if (exception != null) {
                throw exception;
            }
            return response;
        }
    }
}
