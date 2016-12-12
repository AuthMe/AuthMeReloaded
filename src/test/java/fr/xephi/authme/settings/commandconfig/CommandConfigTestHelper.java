package fr.xephi.authme.settings.commandconfig;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Helper class for tests around the command configuration.
 */
final class CommandConfigTestHelper {

    private CommandConfigTestHelper() {
    }

    /**
     * Returns a matcher for verifying a {@link Command} object.
     *
     * @param cmd the expected command line
     * @param executor the expected executor
     * @return the matcher
     */
    static Matcher<Command> isCommand(String cmd, Executor executor) {
        return new TypeSafeMatcher<Command>() {
            @Override
            protected boolean matchesSafely(Command item) {
                return executor.equals(item.getExecutor()) && cmd.equals(item.getCommand());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Command '" + cmd + "' run by '" + executor + "'");
            }
        };
    }
}
