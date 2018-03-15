package fr.xephi.authme.service;

import com.maxmind.db.GeoIp2Provider;
import com.maxmind.db.model.Country;
import com.maxmind.db.model.CountryResponse;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link GeoIpService}.
 */
@RunWith(MockitoJUnitRunner.class)
public class GeoIpServiceTest {

    private GeoIpService geoIpService;
    private File dataFolder;

    @Mock
    private GeoIp2Provider lookupService;

    @Mock
    private BukkitService bukkitService;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void initializeGeoLiteApi() throws IOException {
        dataFolder = temporaryFolder.newFolder();
        geoIpService = new GeoIpService(dataFolder, bukkitService, lookupService);
    }

    @Test
    public void shouldGetCountry() throws Exception {
        // given
        InetAddress ip = InetAddress.getByName("123.45.67.89");
        String countryCode = "XX";

        Country country = mock(Country.class);
        given(country.getIsoCode()).willReturn(countryCode);

        CountryResponse response = mock(CountryResponse.class);
        given(response.getCountry()).willReturn(country);
        given(lookupService.getCountry(ip)).willReturn(response);

        // when
        String result = geoIpService.getCountryCode(ip.getHostAddress());

        // then
        assertThat(result, equalTo(countryCode));
        verify(lookupService).getCountry(ip);
    }

    @Test
    public void shouldNotLookUpCountryForLocalhostIp() throws Exception  {
        // given
        String ip = "127.0.0.1";

        // when
        String result = geoIpService.getCountryCode(ip);

        // then
        assertThat(result, equalTo("--"));
        verify(lookupService, never()).getCountry(any());
    }

    @Test
    public void shouldLookUpCountryName() throws Exception {
        // given
        InetAddress ip = InetAddress.getByName("24.45.167.89");
        String countryName = "Ecuador";

        Country country = mock(Country.class);
        given(country.getName()).willReturn(countryName);

        CountryResponse response = mock(CountryResponse.class);
        given(response.getCountry()).willReturn(country);
        given(lookupService.getCountry(ip)).willReturn(response);

        // when
        String result = geoIpService.getCountryName(ip.getHostAddress());

        // then
        assertThat(result, equalTo(countryName));
        verify(lookupService).getCountry(ip);
    }

    @Test
    public void shouldNotLookUpCountryNameForLocalhostIp() throws Exception {
        // given
        InetAddress ip = InetAddress.getByName("127.0.0.1");

        // when
        String result = geoIpService.getCountryName(ip.getHostAddress());

        // then
        assertThat(result, equalTo("N/A"));
        verify(lookupService, never()).getCountry(ip);
    }
}
