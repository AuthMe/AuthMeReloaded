package fr.xephi.authme.service;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CountryResponse;
import com.maxmind.geoip2.record.Continent;
import com.maxmind.geoip2.record.Country;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.ProtectionSettings;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Test for {@link GeoIpService}.
 */
@RunWith(MockitoJUnitRunner.class)
public class GeoIpServiceTest {

    private GeoIpService geoIpService;

    @Mock
    private DatabaseReader lookupService;

    @Mock
    private BukkitService bukkitService;

    @Mock
    private Settings settings;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void initializeGeoLiteApi() throws IOException {
        File dataFolder = temporaryFolder.newFolder();
        geoIpService = new GeoIpService(dataFolder, bukkitService, settings, lookupService);
    }

    @Test
    public void shouldGetCountry() throws Exception {
        // given
        InetAddress ip = InetAddress.getByName("123.45.67.89");
        String countryCode = "XX";
        CountryResponse response = createCountryResponse(countryCode, "Unknown");
        given(lookupService.country(ip)).willReturn(response);
        given(settings.getProperty(ProtectionSettings.ENABLE_GEOIP)).willReturn(true);

        // when
        String result = geoIpService.getCountryCode(ip.getHostAddress());

        // then
        assertThat(result, equalTo(countryCode));
        verify(lookupService).country(ip);
    }

    @Test
    public void shouldNotLookUpCountryForLocalhostIp() throws Exception  {
        // given
        String ip = "127.0.0.1";

        // when
        String result = geoIpService.getCountryCode(ip);

        // then
        assertThat(result, equalTo("LOCALHOST"));
        verify(lookupService, never()).country(any());
    }

    @Test
    public void shouldLookUpCountryName() throws Exception {
        // given
        InetAddress ip = InetAddress.getByName("24.45.167.89");
        String countryName = "Ecuador";
        CountryResponse response = createCountryResponse("EC", countryName);
        given(lookupService.country(ip)).willReturn(response);
        given(settings.getProperty(ProtectionSettings.ENABLE_GEOIP)).willReturn(true);

        // when
        String result = geoIpService.getCountryName(ip.getHostAddress());

        // then
        assertThat(result, equalTo(countryName));
        verify(lookupService).country(ip);
    }

    @Test
    public void shouldNotLookUpCountryNameForLocalhostIp() throws Exception {
        // given
        InetAddress ip = InetAddress.getByName("127.0.0.1");

        // when
        String result = geoIpService.getCountryName(ip.getHostAddress());

        // then
        assertThat(result, equalTo("LocalHost"));
        verify(lookupService, never()).country(ip);
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

    private static CountryResponse createCountryResponse(String countryCode, String countryName) {
        List<String> locales = Collections.singletonList("en");
        Continent continent = new Continent(locales, "XX", 1L, Collections.emptyMap());

        Map<String, String> countryNames = ImmutableMap.of("en", countryName);
        Country country = new Country(locales, 100, 3L, false, countryCode, countryNames);
        return new CountryResponse(continent, country, null, country, null, null);
    }
}
