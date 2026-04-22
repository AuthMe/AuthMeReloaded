package tools.utils;

import java.util.Scanner;

/**
 * Interface for tasks that can be run automatically, i.e. without any user input.
 */
public interface AutoToolTask extends ToolTask {

    /**
     * Execute the task with default settings.
     */
    void executeDefault();

    @Override
    default void execute(Scanner scanner) {
        executeDefault();
    }

}
