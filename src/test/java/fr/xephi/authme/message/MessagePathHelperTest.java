package fr.xephi.authme.message;

import fr.xephi.authme.settings.properties.PluginSettings;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link MessagePathHelper}.
 */
public class MessagePathHelperTest {

    @Test
    public void shouldHaveLanguageInSyncWithConfigurations() {
        // given / when / then
        assertThat(MessagePathHelper.DEFAULT_LANGUAGE, equalTo(PluginSettings.MESSAGES_LANGUAGE.getDefaultValue()));
        assertThat(MessagePathHelper.DEFAULT_MESSAGES_FILE, equalTo(MessagePathHelper.createMessageFilePath(MessagePathHelper.DEFAULT_LANGUAGE)));
    }

    @Test
    public void shouldBuildTextFilePaths() {
        // given / when / then
        assertThat(MessagePathHelper.createMessageFilePath("qq"), equalTo(MessagePathHelper.MESSAGES_FOLDER + "messages_qq.yml"));
        assertThat(MessagePathHelper.createHelpMessageFilePath("qq"), equalTo(MessagePathHelper.MESSAGES_FOLDER + "help_qq.yml"));
    }

    @Test
    public void shouldRecognizeIfIsMessagesFile() {
        // given / when / then
        assertThat(MessagePathHelper.isMessagesFile("messages_nl.yml"), equalTo(true));
        assertThat(MessagePathHelper.isMessagesFile("messages_testtest.yml"), equalTo(true));

        assertThat(MessagePathHelper.isMessagesFile("messages/messages_fr.yml"), equalTo(false));
        assertThat(MessagePathHelper.isMessagesFile("Messages_fr.yml"), equalTo(false));
        assertThat(MessagePathHelper.isMessagesFile("otherfile.txt"), equalTo(false));
        assertThat(MessagePathHelper.isMessagesFile("messages_de.txt"), equalTo(false));
        assertThat(MessagePathHelper.isMessagesFile(""), equalTo(false));
    }

    @Test
    public void shouldReturnLanguageForMessagesFile() {
        // given / when / then
        assertThat(MessagePathHelper.getLanguageIfIsMessagesFile("messages_nl.yml"), equalTo("nl"));
        assertThat(MessagePathHelper.getLanguageIfIsMessagesFile("messages_testtest.yml"), equalTo("testtest"));

        assertThat(MessagePathHelper.getLanguageIfIsMessagesFile("messages/messages_fr.yml"), nullValue());
        assertThat(MessagePathHelper.getLanguageIfIsMessagesFile("Messages_fr.yml"), nullValue());
        assertThat(MessagePathHelper.getLanguageIfIsMessagesFile("otherfile.txt"), nullValue());
        assertThat(MessagePathHelper.getLanguageIfIsMessagesFile("messages_de.txt"), nullValue());
        assertThat(MessagePathHelper.getLanguageIfIsMessagesFile(""), nullValue());
    }

    @Test
    public void shouldRecognizeIfIsHelpFile() {
        // given / when / then
        assertThat(MessagePathHelper.isHelpFile("help_nl.yml"), equalTo(true));
        assertThat(MessagePathHelper.isHelpFile("help_testtest.yml"), equalTo(true));

        assertThat(MessagePathHelper.isHelpFile("messages/help_fr.yml"), equalTo(false));
        assertThat(MessagePathHelper.isHelpFile("Help_fr.yml"), equalTo(false));
        assertThat(MessagePathHelper.isHelpFile("otherfile.txt"), equalTo(false));
        assertThat(MessagePathHelper.isHelpFile("help_de.txt"), equalTo(false));
        assertThat(MessagePathHelper.isHelpFile(""), equalTo(false));
    }
}
