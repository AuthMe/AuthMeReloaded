package fr.xephi.authme.util;

import com.google.common.base.Strings;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link ValidationService}.
 */
public class ValidationServiceTest {

    private ValidationService validationService;

    @Before
    public void createService() {
        NewSetting settings = mock(NewSetting.class);
        given(settings.getProperty(RestrictionSettings.ALLOWED_PASSWORD_REGEX)).willReturn("[a-zA-Z]+");
        given(settings.getProperty(SecuritySettings.MIN_PASSWORD_LENGTH)).willReturn(3);
        given(settings.getProperty(SecuritySettings.MAX_PASSWORD_LENGTH)).willReturn(20);
        given(settings.getProperty(SecuritySettings.UNSAFE_PASSWORDS))
            .willReturn(Arrays.asList("unsafe", "other-unsafe"));
        validationService = new ValidationService(settings);
    }

    @Test
    public void shouldRejectPasswordSameAsUsername() {
        // given/when
        MessageKey error = validationService.validatePassword("bobby", "Bobby");

        // then
        assertThat(error, equalTo(MessageKey.PASSWORD_IS_USERNAME_ERROR));
    }

    @Test
    public void shouldRejectPasswordNotMatchingPattern() {
        // given/when
        // service mock returns pattern a-zA-Z -> numbers should not be accepted
        MessageKey error = validationService.validatePassword("invalid1234", "myPlayer");

        // then
        assertThat(error, equalTo(MessageKey.PASSWORD_MATCH_ERROR));
    }

    @Test
    public void shouldRejectTooShortPassword() {
        // given/when
        MessageKey error = validationService.validatePassword("ab", "tester");

        // then
        assertThat(error, equalTo(MessageKey.INVALID_PASSWORD_LENGTH));
    }

    @Test
    public void shouldRejectTooLongPassword() {
        // given/when
        MessageKey error = validationService.validatePassword(Strings.repeat("a", 30), "player");

        // then
        assertThat(error, equalTo(MessageKey.INVALID_PASSWORD_LENGTH));
    }

    @Test
    public void shouldRejectUnsafePassword() {
        // given/when
        MessageKey error = validationService.validatePassword("unsafe", "playertest");

        // then
        assertThat(error, equalTo(MessageKey.PASSWORD_UNSAFE_ERROR));
    }

    @Test
    public void shouldAcceptValidPassword() {
        // given/when
        MessageKey error = validationService.validatePassword("safePass", "some_user");

        // then
        assertThat(error, nullValue());
    }

}
