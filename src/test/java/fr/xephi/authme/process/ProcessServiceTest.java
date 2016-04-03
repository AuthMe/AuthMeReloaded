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

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link ProcessService}.
 */
public class ProcessServiceTest {

    private ProcessService processService;
    private Map<Class<?>, Object> mocks;

    @Before
    public void setUpService() {
        mocks = new HashMap<>();
        processService = new ProcessService(newMock(NewSetting.class), newMock(Messages.class), newMock(AuthMe.class),
            newMock(DataSource.class), newMock(IpAddressManager.class), newMock(PasswordSecurity.class),
            newMock(PluginHooks.class), newMock(SpawnLoader.class), newMock(ValidationService.class));
    }

    @Test
    public void shouldGetProperty() {
        // given
        NewSetting settings = getMock(NewSetting.class);
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
        NewSetting settings = processService.getSettings();

        // then
        assertThat(settings, equalTo(getMock(NewSetting.class)));
    }

    @Test
    public void shouldSendMessageToPlayer() {
        // given
        Messages messages = getMock(Messages.class);
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
        Messages messages = getMock(Messages.class);
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
        Messages messages = getMock(Messages.class);
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
        Messages messages = getMock(Messages.class);
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
        AuthMe authMe = processService.getAuthMe();

        // then
        assertThat(authMe, equalTo(getMock(AuthMe.class)));
    }

    @Test
    public void shouldReturnPluginHooks() {
        // given / when
        PluginHooks pluginHooks = processService.getPluginHooks();

        // then
        assertThat(pluginHooks, equalTo(getMock(PluginHooks.class)));
    }

    @Test
    public void shouldReturnIpAddressManager() {
        // given / when
        IpAddressManager ipAddressManager = processService.getIpAddressManager();

        // then
        assertThat(ipAddressManager, equalTo(getMock(IpAddressManager.class)));
    }

    @Test
    public void shouldReturnSpawnLoader() {
        // given / when
        SpawnLoader spawnLoader = processService.getSpawnLoader();

        // then
        assertThat(spawnLoader, equalTo(getMock(SpawnLoader.class)));
    }

    @Test
    public void shouldReturnDatasource() {
        // given / when
        DataSource dataSource = processService.getDataSource();

        // then
        assertThat(dataSource, equalTo(getMock(DataSource.class)));
    }

    @Test
    public void shouldComputeHash() {
        // given
        PasswordSecurity passwordSecurity = getMock(PasswordSecurity.class);
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
        ValidationService validationService = getMock(ValidationService.class);
        given(validationService.validatePassword(password, user)).willReturn(MessageKey.PASSWORD_MATCH_ERROR);

        // when
        MessageKey result = processService.validatePassword(password, user);

        // then
        assertThat(result, equalTo(MessageKey.PASSWORD_MATCH_ERROR));
        verify(validationService).validatePassword(password, user);
    }

    private <T> T newMock(Class<T> clazz) {
        T mock = mock(clazz);
        mocks.put(clazz, mock);
        return mock;
    }

    private <T> T getMock(Class<T> clazz) {
        Object mock = mocks.get(clazz);
        if (mock == null) {
            throw new IllegalArgumentException("No mock of type " + clazz);
        }
        return clazz.cast(mock);
    }
}
