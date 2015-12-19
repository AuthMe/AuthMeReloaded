package fr.xephi.authme.command.help;

import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.ExecutableCommand;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link HelpProvider}.
 */
public class HelpProviderTest {

    private static CommandDescription parent;
    private static CommandDescription child;

    @BeforeClass
    public static void setUpCommands() {
        parent = CommandDescription.builder()
            .executableCommand(mock(ExecutableCommand.class))
            .labels("base", "b")
            .description("Parent")
            .detailedDescription("Test base command.")
            .build();
        child = CommandDescription.builder()
            .executableCommand(mock(ExecutableCommand.class))
            .parent(parent)
            .labels("child", "c")
            .description("Child")
            .detailedDescription("Child test command.")
            .withArgument("Argument", "The argument", false)
            .build();
    }

    @Test
    public void shouldRetainCorrectLabels() {
        // given
        List<String> labels = Arrays.asList("b", "child");

        // when
        List<String> result = HelpProvider.filterCorrectLabels(child, labels);

        // then
        assertThat(result, contains("b", "child"));
    }

    @Test
    public void shouldReplaceIncorrectLabels() {
        // given
        List<String> labels = Arrays.asList("base", "wrong");

        // when
        List<String> result = HelpProvider.filterCorrectLabels(child, labels);

        // then
        assertThat(result, contains("base", "child"));
    }

}
