package tools.utils;

import java.util.Scanner;

/**
 * Abstract class for auto tool tasks that perform exactly the same action for
 * {@link ToolTask#execute(Scanner)} and {@link AutoToolTask#executeDefault()}.
 */
public abstract class SimpleAutoTask implements AutoToolTask {

    @Override
    public final void execute(Scanner scanner) {
        executeDefault();
    }
}
