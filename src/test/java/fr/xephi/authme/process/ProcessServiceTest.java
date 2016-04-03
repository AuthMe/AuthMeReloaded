package fr.xephi.authme.process;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.IpAddressManager;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.hooks.PluginHooks;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.ValidationService;
import org.bukkit.command.CommandSender;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link ProcessService}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProcessServiceTest {

    private ProcessService processService;
    @Mock
    private ValidationService validationService;
    @Mock
    private NewSetting settings;
    @Mock
    private Messages messages;
    @Mock
    private IpAddressManager ipAddressManager;
    @Mock
    private PasswordSecurity passwordSecurity;
    @Mock
    private AuthMe authMe;
    @Mock
    private DataSource dataSource;
    @Mock
    private SpawnLoader spawnLoader;
    @Mock
    private PluginHooks pluginHooks;

    @Before
    public void setUpService() {
        processService = new ProcessService(settings, messages, authMe, dataSource, ipAddressManager, passwordSecurity,
            pluginHooks, spawnLoader, validationService);
    }

    @Test
    public void shouldGetProperty() {
        // given
        given(settings.getProperty(SecuritySettings.CAPTCHA_LENGTH)).willReturn(8);

        // when
        int result = processService.getProperty(SecuritySettings.CAPTCHA_LENGTH);

        // then
        verify(settings).getProperty(SecuritySettings.CAPTCHA_LENGTH);
        assertThat(result, equalTo(8));
    }

    @Test
    public void shouldReturnSettings() {
        // given/when
        NewSetting result = processService.getSettings();

        // then
        assertThat(result, equalTo(settings));
    }

    @Test
    public void shouldSendMessageToPlayer() {
        // given
        CommandSender sender = mock(CommandSender.class);
        MessageKey key = MessageKey.ACCOUNT_NOT_ACTIVATED;

        // when
        processService.send(sender, key);

        // then
        verify(messages).send(sender, key);
    }

    @Test
    public void shouldSendMessageWithReplacements() {
        // given
        CommandSender sender = mock(CommandSender.class);
        MessageKey key = MessageKey.ACCOUNT_NOT_ACTIVATED;
        String[] replacements = new String[]{"test", "toast"};

        // when
        processService.send(sender, key, replacements);

        // then
        verify(messages).send(sender, key, replacements);
    }

    @Test
    public void shouldRetrieveMessage() {
        // given
        MessageKey key = MessageKey.ACCOUNT_NOT_ACTIVATED;
        String[] lines = new String[]{"First message line", "second line"};
        given(messages.retrieve(key)).willReturn(lines);

        // when
        String[] result = processService.retrieveMessage(key);

        // then
        assertThat(result, equalTo(lines));
        verify(messages).retrieve(key);
    }

    @Test
    public void shouldRetrieveSingleMessage() {
        // given
        MessageKey key = MessageKey.ACCOUNT_NOT_ACTIVATED;
        String text = "Test text";
        given(messages.retrieveSingle(key)).willReturn(text);

        // when
        String result = processService.retrieveSingleMessage(key);

        // then
        assertThat(result, equalTo(text));
        verify(messages).retrieveSingle(key);
    }

    @Test
    public void shouldReturnAuthMeInstance() {
        // given / when
        AuthMe result = processService.getAuthMe();

        // then
        assertThat(result, equalTo(authMe));
    }

    @Test
    public void shouldReturnPluginHooks() {
        // given / when
        PluginHooks result = processService.getPluginHooks();

        // then
        assertThat(result, equalTo(pluginHooks));
    }

    @Test
    public void shouldReturnIpAddressManager() {
        // given / when
        IpAddressManager result = processService.getIpAddressManager();

        // then
        assertThat(result, equalTo(ipAddressManager));
    }

    @Test
    public void shouldReturnSpawnLoader() {
        // given / when
        SpawnLoader result = processService.getSpawnLoader();

        // then
        assertThat(result, equalTo(spawnLoader));
    }

    @Test
    public void shouldReturnDatasource() {
        // given / when
        DataSource result = processService.getDataSource();

        // then
        assertThat(result, equalTo(dataSource));
    }

    @Test
    public void shouldComputeHash() {
        // given
        String password = "test123";
        String username = "Username";
        HashedPassword hashedPassword = new HashedPassword("hashedResult", "salt12342");
        given(passwordSecurity.computeHash(password, username)).willReturn(hashedPassword);

        // when
        HashedPassword result = processService.computeHash(password, username);

        // then
        assertThat(result, equalTo(hashedPassword));
        verify(passwordSecurity).computeHash(password, username);
    }

    @Test
    public void shouldValidatePassword() {
        // given
        String user = "test-user";
        String password = "passw0rd";
        given(validationService.validatePassword(password, user)).willReturn(MessageKey.PASSWORD_MATCH_ERROR);

        // when
        MessageKey result = processService.validatePassword(password, user);

        // then
        assertThat(result, equalTo(MessageKey.PASSWORD_MATCH_ERROR));
        verify(validationService).validatePassword(password, user);
    }

    @Test
    public void shouldValidateEmail() {
        // given
        String email = "test@example.tld";
        given(validationService.validateEmail(email)).willReturn(true);

        // when
        boolean result = processService.validateEmail(email);

        // then
        assertThat(result, equalTo(true));
        verify(validationService).validateEmail(email);
    }

    @Test
    public void shouldCheckIfEmailCanBeUsed() {
        // given
        String email = "mail@example.com";
        CommandSender sender = mock(CommandSender.class);
        given(validationService.isEmailFreeForRegistration(email, sender))
            .willReturn(true);

        // when
        boolean result = processService.isEmailFreeForRegistration(email, sender);

        // then
        assertThat(result, equalTo(true));
        verify(validationService).isEmailFreeForRegistration(email, sender);
    }
}
