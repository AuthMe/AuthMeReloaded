package utils;

import java.util.Map;

/**
 * Common interface for tool tasks. Note that the implementing tasks are instantiated
 * with the default constructor. It is required that it be public.
 */
public interface ToolTask {

    void execute(Map<String, String> options);

    String getTaskName();

    Iterable<TaskOption> getOptions();

}
