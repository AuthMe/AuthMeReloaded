package fr.xephi.authme.service;

import com.maxmind.db.GeoIp2Provider;
import com.maxmind.db.model.Country;
import com.maxmind.db.model.CountryResponse;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.ProtectionSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.net.InetAddress;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Test for {@link GeoIpService}.
 */
@ExtendWith(MockitoExtension.class)
class GeoIpServiceTest {

    private GeoIpService geoIpService;

    @TempDir
    File dataFolder;

    @Mock
    private GeoIp2Provider lookupService;

    @Mock
    private BukkitService bukkitService;

    @Mock
    private Settings settings;

    @BeforeEach
    void initializeGeoLiteApi() {
        geoIpService = new GeoIpService(dataFolder, bukkitService, settings, lookupService);
    }

    @Test
    void shouldGetCountry() throws Exception {
        // given
        InetAddress ip = InetAddress.getByName("123.45.67.89");
        String countryCode = "XX";

        Country country = mock(Country.class);
        given(country.getIsoCode()).willReturn(countryCode);

        CountryResponse response = mock(CountryResponse.class);
        given(response.getCountry()).willReturn(country);
        given(lookupService.getCountry(ip)).willReturn(response);
        given(settings.getProperty(ProtectionSettings.ENABLE_GEOIP)).willReturn(true);

        // when
        String result = geoIpService.getCountryCode(ip.getHostAddress());

        // then
        assertThat(result, equalTo(countryCode));
        verify(lookupService).getCountry(ip);
    }

    @Test
    void shouldNotLookUpCountryForLocalhostIp() throws Exception  {
        // given
        String ip = "127.0.0.1";

        // when
        String result = geoIpService.getCountryCode(ip);

        // then
        assertThat(result, equalTo("LOCALHOST"));
        verify(lookupService, never()).getCountry(any());
    }

    @Test
    void shouldLookUpCountryName() throws Exception {
        // given
        InetAddress ip = InetAddress.getByName("24.45.167.89");
        String countryName = "Ecuador";

        Country country = mock(Country.class);
        given(country.getName()).willReturn(countryName);

        CountryResponse response = mock(CountryResponse.class);
        given(response.getCountry()).willReturn(country);
        given(lookupService.getCountry(ip)).willReturn(response);
        given(settings.getProperty(ProtectionSettings.ENABLE_GEOIP)).willReturn(true);

        // when
        String result = geoIpService.getCountryName(ip.getHostAddress());

        // then
        assertThat(result, equalTo(countryName));
        verify(lookupService).getCountry(ip);
    }

    @Test
    void shouldNotLookUpCountryNameForLocalhostIp() throws Exception {
        // given
        InetAddress ip = InetAddress.getByName("127.0.0.1");

        // when
        String result = geoIpService.getCountryName(ip.getHostAddress());

        // then
        assertThat(result, equalTo("LocalHost"));
        verify(lookupService, never()).getCountry(ip);
    }

    @Test
    public void shouldNotLookUpCountryNameIfDisabled() throws Exception {
        // given
        InetAddress ip = InetAddress.getByName("24.45.167.89");
        given(settings.getProperty(ProtectionSettings.ENABLE_GEOIP)).willReturn(false);

        // when
        String result = geoIpService.getCountryName(ip.getHostAddress());

        // then
        assertThat(result, equalTo("N/A"));
        verifyNoInteractions(lookupService);
    }
}
