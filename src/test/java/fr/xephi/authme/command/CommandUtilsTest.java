package fr.xephi.authme.command;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link CommandUtils}.
 */
public class CommandUtilsTest {

    @Test
    public void shouldPrintLabels() {
        // given
        List<String> labels = Arrays.asList("authme", "help", "reload");

        // when
        String result = CommandUtils.labelsToString(labels);

        // then
        assertThat(result, equalTo("authme help reload"));
    }

    @Test
    public void shouldReturnCommandPath() {
        // given
        CommandDescription base = CommandDescription.builder()
            .labels("authme", "auth")
            .description("Base")
            .detailedDescription("Test base command.")
            .executableCommand(mock(ExecutableCommand.class))
            .build();
        CommandDescription command = CommandDescription.builder()
            .parent(base)
            .labels("help", "h", "?")
            .description("Child")
            .detailedDescription("Test child command.")
            .executableCommand(mock(ExecutableCommand.class))
            .build();

        // when
        String commandPath = CommandUtils.constructCommandPath(command);

        // then
        assertThat(commandPath, equalTo("/authme help"));
    }
}
