package fr.xephi.authme.settings;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.AuthMeMockUtil;
import fr.xephi.authme.ConsoleLogger;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static java.util.Arrays.asList;

/**
 * Test for {@link Messages}.
 */
public class MessagesTest {

    private static final String YML_TEST_FILE = "messages_test.yml";
    private Messages messages;

    @Before
    public void setUpMessages() {
        AuthMe authMe = AuthMeMockUtil.mockAuthMeInstance();
        AuthMeMockUtil.insertMockWrapperInstance(ConsoleLogger.class, "wrapper", authMe);

        Settings.messagesLanguage = "en";
        URL url = getClass().getClassLoader().getResource(YML_TEST_FILE);
        if (url == null) {
            throw new RuntimeException("File '" + YML_TEST_FILE + "' could not be loaded");
        }

        File file = new File(url.getFile());
        messages = new Messages(file, "en");
    }

    @Test
    public void shouldLoadMessages() {
        // given
        // when
        String[] send = messages.send(MessageKey.CAPTCHA_WRONG_ERROR.getKey());
        System.out.println(asList(send));
    }
}
