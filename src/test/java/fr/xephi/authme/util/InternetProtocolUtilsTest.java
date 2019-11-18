package fr.xephi.authme.util;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link InternetProtocolUtils}
 */
public class InternetProtocolUtilsTest {

    @Test
    public void shouldCheckLocalAddress() {
        // loopback
        assertThat(InternetProtocolUtils.isLocalAddress("localhost"), equalTo(true));
        assertThat(InternetProtocolUtils.isLocalAddress("127.0.0.1"), equalTo(true));
        assertThat(InternetProtocolUtils.isLocalAddress("::1"), equalTo(true));

        // site local
        assertThat(InternetProtocolUtils.isLocalAddress("10.0.0.1"), equalTo(true));
        assertThat(InternetProtocolUtils.isLocalAddress("172.0.0.1"), equalTo(false));
        assertThat(InternetProtocolUtils.isLocalAddress("172.16.0.1"), equalTo(true));
        assertThat(InternetProtocolUtils.isLocalAddress("192.168.0.1"), equalTo(true));

        // deprecated site-local
        // ref: https://en.wikipedia.org/wiki/IPv6_address#Default_address_selection
        assertThat(InternetProtocolUtils.isLocalAddress("fec0::"), equalTo(true));

        // unique site-local (not deprecated!)
        // ref: https://en.wikipedia.org/wiki/Unique_local_address
        assertThat(InternetProtocolUtils.isLocalAddress("fde4:8dba:82e1::"), equalTo(true));
        assertThat(InternetProtocolUtils.isLocalAddress("fc00::"), equalTo(true));
        assertThat(InternetProtocolUtils.isLocalAddress("fdff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"), equalTo(true));
        assertThat(InternetProtocolUtils.isLocalAddress("fe00::"), equalTo(false));

        // link local
        assertThat(InternetProtocolUtils.isLocalAddress("169.254.0.64"), equalTo(true));
        assertThat(InternetProtocolUtils.isLocalAddress("FE80:0000:0000:0000:C800:0EFF:FE74:0008"), equalTo(true));

        // public
        assertThat(InternetProtocolUtils.isLocalAddress("94.32.34.5"), equalTo(false));
    }

    @Test
    public void testIsLoopback() {
        // loopback
        assertThat(InternetProtocolUtils.isLoopbackAddress("localhost"), equalTo(true));
        assertThat(InternetProtocolUtils.isLoopbackAddress("127.0.0.1"), equalTo(true));
        assertThat(InternetProtocolUtils.isLoopbackAddress("::1"), equalTo(true));
    }
}
