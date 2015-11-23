package fr.xephi.authme.settings;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.AuthMeMockUtil;
import fr.xephi.authme.ConsoleLogger;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 * Test for {@link Messages}.
 */
public class MessagesTest {

    @Before
    public void setUpMessages() {
        AuthMe authMe = AuthMeMockUtil.mockAuthMeInstance();
        AuthMeMockUtil.insertMockWrapperInstance(ConsoleLogger.class, "wrapper", authMe);
        File file = new File("messages_test.yml");
        Messages messages = new Messages(file, "en");
    }

    @Test
    public void shouldLoadMessages() {

    }
}
