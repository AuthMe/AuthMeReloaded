package fr.xephi.authme.settings;

import ch.jalu.configme.configurationdata.ConfigurationData;
import ch.jalu.configme.configurationdata.ConfigurationDataBuilder;
import ch.jalu.configme.resource.PropertyReader;
import ch.jalu.configme.resource.PropertyResource;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.settings.properties.TestConfiguration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link Settings}.
 */
public class SettingsTest {
    
    private static final ConfigurationData CONFIG_DATA =
        ConfigurationDataBuilder.createConfiguration(TestConfiguration.class);

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private File testPluginFolder;

    @BeforeClass
    public static void setUpLogger() {
        TestHelper.setupLogger();
    }

    @Before
    public void setUpTestPluginFolder() throws IOException {
        testPluginFolder = temporaryFolder.newFolder();
    }

    @Test
    public void shouldLoadEmailMessage() throws IOException {
        // given
        String emailMessage = "Sample email message\nThat's all!";
        File emailFile = new File(testPluginFolder, "email.html");
        createFile(emailFile);
        Files.write(emailFile.toPath(), emailMessage.getBytes());

        PropertyResource resource = mockPropertyResourceAndReader();
        Settings settings = new Settings(testPluginFolder, resource, null, CONFIG_DATA);

        // when
        String result = settings.getPasswordEmailMessage();

        // then
        assertThat(result, equalTo(emailMessage));
    }

    @Test
    public void shouldLoadRecoveryCodeMessage() throws IOException {
        // given
        String emailMessage = "Your recovery code is %code.";
        File emailFile = new File(testPluginFolder, "recovery_code_email.html");
        createFile(emailFile);
        Files.write(emailFile.toPath(), emailMessage.getBytes());

        PropertyResource resource = mockPropertyResourceAndReader();
        Settings settings = new Settings(testPluginFolder, resource, null, CONFIG_DATA);

        // when
        String result = settings.getRecoveryCodeEmailMessage();

        // then
        assertThat(result, equalTo(emailMessage));
    }

    @Test
    public void shouldLoadVerificationMessage() throws IOException {
        // given
        String emailMessage = "Please verify your identity with <recoverycode />.";
        File emailFile = new File(testPluginFolder, "verification_code_email.html");
        createFile(emailFile);
        Files.write(emailFile.toPath(), emailMessage.getBytes());

        PropertyResource resource = mockPropertyResourceAndReader();
        Settings settings = new Settings(testPluginFolder, resource, null, CONFIG_DATA);

        // when
        String result = settings.getVerificationEmailMessage();

        // then
        assertThat(result, equalTo(emailMessage));
    }

    private static PropertyResource mockPropertyResourceAndReader() {
        PropertyReader reader = mock(PropertyReader.class, RETURNS_DEEP_STUBS);
        given(reader.getList(anyString())).willReturn(Collections.emptyList());
        PropertyResource resource = mock(PropertyResource.class);
        given(resource.createReader()).willReturn(reader);
        return resource;
    }

    private static void createFile(File file) {
        try {
            file.getParentFile().mkdirs();
            file.createNewFile();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
