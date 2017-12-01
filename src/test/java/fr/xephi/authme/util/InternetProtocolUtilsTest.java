package fr.xephi.authme.util;

import fr.xephi.authme.TestHelper;
import org.junit.Test;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Test for {@link InternetProtocolUtils}
 */
public class InternetProtocolUtilsTest {

    @Test
    public void shouldCheckLocalAddress() {
        assertThat(InternetProtocolUtils.isLocalAddress("127.0.0.1"), equalTo(true));
        assertThat(InternetProtocolUtils.isLocalAddress("10.0.0.1"), equalTo(true));
        assertThat(InternetProtocolUtils.isLocalAddress("172.0.0.1"), equalTo(false));
        assertThat(InternetProtocolUtils.isLocalAddress("172.16.0.1"), equalTo(true));
        assertThat(InternetProtocolUtils.isLocalAddress("192.168.0.1"), equalTo(true));
        assertThat(InternetProtocolUtils.isLocalAddress("94.32.34.5"), equalTo(false));
    }

    @Test
    public void shouldHavePrivateConstructor() {
        // given / when / then
        TestHelper.validateHasOnlyPrivateEmptyConstructor(InternetProtocolUtils.class);
    }
}
