package fr.xephi.authme.util;

import com.google.common.base.Strings;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.runner.BeforeInjecting;
import fr.xephi.authme.runner.DelayedInjectionRunner;
import fr.xephi.authme.runner.InjectDelayed;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.ValidationService.ValidationResult;
import org.bukkit.command.CommandSender;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link ValidationService}.
 */
@RunWith(DelayedInjectionRunner.class)
public class ValidationServiceTest {

    @InjectDelayed
    private ValidationService validationService;
    @Mock
    private NewSetting settings;
    @Mock
    private DataSource dataSource;
    @Mock
    private PermissionsManager permissionsManager;
    @Mock
    private GeoLiteAPI geoLiteApi;

    @BeforeInjecting
    public void createService() {
        given(settings.getProperty(RestrictionSettings.ALLOWED_PASSWORD_REGEX)).willReturn("[a-zA-Z]+");
        given(settings.getProperty(SecuritySettings.MIN_PASSWORD_LENGTH)).willReturn(3);
        given(settings.getProperty(SecuritySettings.MAX_PASSWORD_LENGTH)).willReturn(20);
        given(settings.getProperty(SecuritySettings.UNSAFE_PASSWORDS))
            .willReturn(Arrays.asList("unsafe", "other-unsafe"));
        given(settings.getProperty(EmailSettings.MAX_REG_PER_EMAIL)).willReturn(3);
    }

    @Test
    public void shouldRejectPasswordSameAsUsername() {
        // given/when
        ValidationResult error = validationService.validatePassword("bobby", "Bobby");

        // then
        assertErrorEquals(error, MessageKey.PASSWORD_IS_USERNAME_ERROR);
    }

    @Test
    public void shouldRejectPasswordNotMatchingPattern() {
        // given/when
        // service mock returns pattern a-zA-Z -> numbers should not be accepted
        ValidationResult error = validationService.validatePassword("invalid1234", "myPlayer");

        // then
        assertErrorEquals(error, MessageKey.PASSWORD_CHARACTERS_ERROR, "[a-zA-Z]+");
    }

    @Test
    public void shouldRejectTooShortPassword() {
        // given/when
        ValidationResult error = validationService.validatePassword("ab", "tester");

        // then
        assertErrorEquals(error, MessageKey.INVALID_PASSWORD_LENGTH);
    }

    @Test
    public void shouldRejectTooLongPassword() {
        // given/when
        ValidationResult error = validationService.validatePassword(Strings.repeat("a", 30), "player");

        // then
        assertErrorEquals(error, MessageKey.INVALID_PASSWORD_LENGTH);
    }

    @Test
    public void shouldRejectUnsafePassword() {
        // given/when
        ValidationResult error = validationService.validatePassword("unsafe", "playertest");

        // then
        assertErrorEquals(error, MessageKey.PASSWORD_UNSAFE_ERROR);
    }

    @Test
    public void shouldAcceptValidPassword() {
        // given/when
        ValidationResult error = validationService.validatePassword("safePass", "some_user");

        // then
        assertThat(error.hasError(), equalTo(false));
    }

    @Test
    public void shouldAcceptEmailWithEmptyLists() {
        // given
        given(settings.getProperty(EmailSettings.DOMAIN_WHITELIST)).willReturn(Collections.<String>emptyList());
        given(settings.getProperty(EmailSettings.DOMAIN_BLACKLIST)).willReturn(Collections.<String>emptyList());

        // when
        boolean result = validationService.validateEmail("test@example.org");

        // then
        assertThat(result, equalTo(true));
    }

    @Test
    public void shouldAcceptEmailWithWhitelist() {
        // given
        given(settings.getProperty(EmailSettings.DOMAIN_WHITELIST))
            .willReturn(Arrays.asList("domain.tld", "example.com"));
        given(settings.getProperty(EmailSettings.DOMAIN_BLACKLIST)).willReturn(Collections.<String>emptyList());

        // when
        boolean result = validationService.validateEmail("TesT@Example.com");

        // then
        assertThat(result, equalTo(true));
    }

    @Test
    public void shouldRejectEmailNotInWhitelist() {
        // given
        given(settings.getProperty(EmailSettings.DOMAIN_WHITELIST))
            .willReturn(Arrays.asList("domain.tld", "example.com"));
        given(settings.getProperty(EmailSettings.DOMAIN_BLACKLIST)).willReturn(Collections.<String>emptyList());

        // when
        boolean result = validationService.validateEmail("email@other-domain.abc");

        // then
        assertThat(result, equalTo(false));
    }

    @Test
    public void shouldAcceptEmailNotInBlacklist() {
        // given
        given(settings.getProperty(EmailSettings.DOMAIN_WHITELIST)).willReturn(Collections.<String>emptyList());
        given(settings.getProperty(EmailSettings.DOMAIN_BLACKLIST))
            .willReturn(Arrays.asList("Example.org", "a-test-name.tld"));

        // when
        boolean result = validationService.validateEmail("sample@valid-name.tld");

        // then
        assertThat(result, equalTo(true));
    }

    @Test
    public void shouldRejectEmailInBlacklist() {
        // given
        given(settings.getProperty(EmailSettings.DOMAIN_WHITELIST)).willReturn(Collections.<String>emptyList());
        given(settings.getProperty(EmailSettings.DOMAIN_BLACKLIST))
            .willReturn(Arrays.asList("Example.org", "a-test-name.tld"));

        // when
        boolean result = validationService.validateEmail("sample@a-Test-name.tld");

        // then
        assertThat(result, equalTo(false));
    }

    @Test
    public void shouldRejectInvalidEmail() {
        // given/when/then
        assertThat(validationService.validateEmail("invalidinput"), equalTo(false));
    }

    @Test
    public void shouldRejectDefaultEmail() {
        // given/when/then
        assertThat(validationService.validateEmail("your@email.com"), equalTo(false));
    }

    public void shouldAllowRegistration() {
        // given
        CommandSender sender = mock(CommandSender.class);
        String email = "my.address@example.org";
        given(permissionsManager.hasPermission(sender, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS))
            .willReturn(false);
        given(dataSource.countAuthsByEmail(email)).willReturn(2);

        // when
        boolean result = validationService.isEmailFreeForRegistration(email, sender);

        // then
        assertThat(result, equalTo(true));
    }

    public void shouldRejectEmailWithTooManyAccounts() {
        // given
        CommandSender sender = mock(CommandSender.class);
        String email = "mail@example.org";
        given(permissionsManager.hasPermission(sender, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS))
            .willReturn(false);
        given(dataSource.countAuthsByEmail(email)).willReturn(5);

        // when
        boolean result = validationService.isEmailFreeForRegistration(email, sender);

        // then
        assertThat(result, equalTo(false));
    }

    public void shouldAllowBypassForPresentPermission() {
        // given
        CommandSender sender = mock(CommandSender.class);
        String email = "mail-address@example.com";
        given(permissionsManager.hasPermission(sender, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS))
            .willReturn(true);
        given(dataSource.countAuthsByEmail(email)).willReturn(7);

        // when
        boolean result = validationService.isEmailFreeForRegistration(email, sender);

        // then
        assertThat(result, equalTo(true));
    }

    private static void assertErrorEquals(ValidationResult validationResult, MessageKey messageKey, String... args) {
        assertThat(validationResult.hasError(), equalTo(true));
        assertThat(validationResult.getMessageKey(), equalTo(messageKey));
        assertThat(validationResult.getArgs(), equalTo(args));
    }
}
