package fr.xephi.authme.command.help;

import com.google.common.io.Files;
import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.TestCommandsUtil;
import fr.xephi.authme.message.HelpMessagesFileHandler;
import fr.xephi.authme.message.MessagePathHelper;
import fr.xephi.authme.permission.DefaultPermission;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import static fr.xephi.authme.command.TestCommandsUtil.getCommandWithLabel;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link HelpMessagesService}.
 */
class HelpMessagesServiceTest {

    private static final String TEST_FILE = TestHelper.PROJECT_ROOT + "command/help/help_test.yml";
    private static final Collection<CommandDescription> COMMANDS = TestCommandsUtil.generateCommands();

    private HelpMessagesService helpMessagesService;

    @TempDir
    File dataFolder;

    @BeforeEach
    void initializeHandler() throws IOException {
        new File(dataFolder, "messages").mkdirs();
        File messagesFile = new File(dataFolder, MessagePathHelper.createHelpMessageFilePath("test"));
        Files.copy(TestHelper.getJarFile(TEST_FILE), messagesFile);

        HelpMessagesFileHandler helpMessagesFileHandler = createMessagesFileHandler();
        helpMessagesService = new HelpMessagesService(helpMessagesFileHandler);
    }

    @Test
    void shouldReturnLocalizedCommand() {
        // given
        CommandDescription command = getCommandWithLabel(COMMANDS, "authme", "register");

        // when
        CommandDescription localCommand = helpMessagesService.buildLocalizedDescription(command);

        // then
        assertThat(localCommand.getDescription(), equalTo("Registration"));
        assertThat(localCommand.getDetailedDescription(), equalTo("Registers the player"));
        assertThat(localCommand.getExecutableCommand(), equalTo(command.getExecutableCommand()));
        assertThat(localCommand.getPermission(), equalTo(command.getPermission()));
        assertThat(localCommand.getArguments(), hasSize(2));
        assertThat(localCommand.getArguments().get(0).getName(), equalTo("password"));
        assertThat(localCommand.getArguments().get(0).getDescription(), equalTo("The password"));
        assertThat(localCommand.getArguments().get(1).getName(), equalTo("confirmation"));
        assertThat(localCommand.getArguments().get(1).getDescription(), equalTo("The confirmation"));
    }

    @Test
    void shouldReturnLocalizedCommandWithDefaults() {
        // given
        CommandDescription command = getCommandWithLabel(COMMANDS, "authme", "login");

        // when
        CommandDescription localCommand = helpMessagesService.buildLocalizedDescription(command);

        // then
        assertThat(localCommand.getDescription(), equalTo("Logging in"));
        assertThat(localCommand.getDetailedDescription(), equalTo("'login' test command"));
        assertThat(localCommand.getArguments(), hasSize(1));
        assertThat(localCommand.getArguments().get(0).getName(), equalTo("user password"));
        assertThat(localCommand.getArguments().get(0).getDescription(), equalTo("'password' argument description"));
    }

    @Test
    void shouldReturnSameCommandForNoLocalization() {
        // given
        CommandDescription command = getCommandWithLabel(COMMANDS, "email");

        // when
        CommandDescription localCommand = helpMessagesService.buildLocalizedDescription(command);

        // then
        assertThat(localCommand, sameInstance(command));
    }

    @Test
    void shouldKeepChildrenInLocalCommand() {
        // given
        CommandDescription command = getCommandWithLabel(COMMANDS, "authme");

        // when
        CommandDescription localCommand = helpMessagesService.buildLocalizedDescription(command);

        // then
        assertThat(localCommand.getChildren(), equalTo(command.getChildren()));
        assertThat(localCommand.getDescription(), equalTo("authme cmd"));
        assertThat(localCommand.getDetailedDescription(), equalTo("Main command"));
    }

    @Test
    void shouldGetTranslationsForSectionAndMessage() {
        // given / when / then
        assertThat(helpMessagesService.getMessage(DefaultPermission.OP_ONLY), equalTo("only op"));
        assertThat(helpMessagesService.getMessage(HelpMessage.RESULT), equalTo("res."));
        assertThat(helpMessagesService.getMessage(HelpSection.ARGUMENTS), equalTo("arg."));
    }

    @Test
    void shouldGetLocalCommandDescription() {
        // given
        CommandDescription command = getCommandWithLabel(COMMANDS, "authme", "register");

        // when
        String description = helpMessagesService.getDescription(command);

        // then
        assertThat(description, equalTo("Registration"));
    }

    @Test
    void shouldFallbackToDescriptionOnCommandObject() {
        // given
        CommandDescription command = getCommandWithLabel(COMMANDS, "unregister");

        // when
        String description = helpMessagesService.getDescription(command);

        // then
        assertThat(description, equalTo(command.getDescription()));
    }

    private HelpMessagesFileHandler createMessagesFileHandler() {
        Settings settings = mock(Settings.class);
        given(settings.getProperty(PluginSettings.MESSAGES_LANGUAGE)).willReturn("test");

        HelpMessagesFileHandler messagesFileHandler = new HelpMessagesFileHandler(dataFolder, settings);
        ReflectionTestUtils.invokePostConstructMethods(messagesFileHandler);
        return messagesFileHandler;
    }
}
