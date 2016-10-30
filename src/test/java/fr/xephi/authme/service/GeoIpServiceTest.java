package fr.xephi.authme.service;

import com.maxmind.geoip.Country;
import com.maxmind.geoip.LookupService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
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
    private LookupService lookupService;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void initializeGeoLiteApi() throws IOException {
        dataFolder = temporaryFolder.newFolder();
        geoIpService = new GeoIpService(dataFolder, lookupService);
    }

    @Test
    public void shouldGetCountry() {
        // given
        String ip = "123.45.67.89";
        String countryCode = "XX";
        Country country = mock(Country.class);
        given(country.getCode()).willReturn(countryCode);
        given(lookupService.getCountry(ip)).willReturn(country);

        // when
        String result = geoIpService.getCountryCode(ip);

        // then
        assertThat(result, equalTo(countryCode));
        verify(lookupService).getCountry(ip);
    }

    @Test
    public void shouldNotLookUpCountryForLocalhostIp() {
        // given
        String ip = "127.0.0.1";

        // when
        String result = geoIpService.getCountryCode(ip);

        // then
        assertThat(result, equalTo("--"));
        verify(lookupService, never()).getCountry(anyString());
    }

    @Test
    public void shouldLookUpCountryName() {
        // given
        String ip = "24.45.167.89";
        String countryName = "Ecuador";
        Country country = mock(Country.class);
        given(country.getName()).willReturn(countryName);
        given(lookupService.getCountry(ip)).willReturn(country);

        // when
        String result = geoIpService.getCountryName(ip);

        // then
        assertThat(result, equalTo(countryName));
        verify(lookupService).getCountry(ip);
    }

    @Test
    public void shouldNotLookUpCountryNameForLocalhostIp() {
        // given
        String ip = "127.0.0.1";

        // when
        String result = geoIpService.getCountryName(ip);

        // then
        assertThat(result, equalTo("N/A"));
        verify(lookupService, never()).getCountry(ip);
    }

}
