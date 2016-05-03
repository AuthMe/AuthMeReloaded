package tools.utils;

import java.util.Scanner;

/**
 * Common interface for tool tasks. Note that the implementing tasks are instantiated
 * with the default constructor. It is required that it be public.
 */
public interface ToolTask {

    /**
     * Return the name of the task.
     *
     * @return Name of the task
     */
    String getTaskName();

    /**
     * Execute the task.
     *
     * @param scanner Scanner to prompt the user with for options. Do not close it.
     */
    void execute(Scanner scanner);

}
