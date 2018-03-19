package fr.xephi.authme.message.updater;

import ch.jalu.configme.configurationdata.ConfigurationData;
import ch.jalu.configme.properties.Property;
import ch.jalu.configme.properties.StringProperty;
import com.google.common.io.Files;
import fr.xephi.authme.TestHelper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link MigraterYamlFileResource}.
 */
public class MigraterYamlFileResourceTest {

    private static final String CHINESE_MESSAGES_FILE = TestHelper.PROJECT_ROOT + "message/chinese_texts.yml";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldReadChineseFile() {
        // given
        File file = TestHelper.getJarFile(CHINESE_MESSAGES_FILE);

        // when
        MigraterYamlFileResource resource = new MigraterYamlFileResource(file);

        // then
        assertThat(resource.getString("first"), equalTo("错误的密码"));
        assertThat(resource.getString("second"), equalTo("为了验证您的身份，您需要将一个电子邮件地址与您的帐户绑定！"));
        assertThat(resource.getString("third"), equalTo("您已经可以在当前会话中执行任何敏感命令！"));
    }

    @Test
    public void shouldWriteWithCorrectCharset() throws IOException {
        // given
        File file = temporaryFolder.newFile();
        Files.copy(TestHelper.getJarFile(CHINESE_MESSAGES_FILE), file);
        MigraterYamlFileResource resource = new MigraterYamlFileResource(file);
        String newMessage = "您当前并没有任何邮箱与该账号绑定";
        resource.setValue("third", newMessage);

        // when
        resource.exportProperties(buildConfigurationData());

        // then
        resource = new MigraterYamlFileResource(file);
        assertThat(resource.getString("first"), equalTo("错误的密码"));
        assertThat(resource.getString("second"), equalTo("为了验证您的身份，您需要将一个电子邮件地址与您的帐户绑定！"));
        assertThat(resource.getString("third"), equalTo(newMessage));
    }

    private static ConfigurationData buildConfigurationData() {
        List<Property<?>> properties = Arrays.asList(
            new StringProperty("first", "first"),
            new StringProperty("second", "second"),
            new StringProperty("third", "third"));
        return new ConfigurationData(properties);
    }
}
