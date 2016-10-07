package tools;

import fr.xephi.authme.ClassCollector;
import fr.xephi.authme.TestHelper;
import tools.utils.AutoToolTask;
import tools.utils.ToolTask;

import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

/**
 * Runner for executing tool tasks.
 */
public final class ToolsRunner {

    private Map<String, ToolTask> tasks;

    private ToolsRunner(Map<String, ToolTask> tasks) {
        this.tasks = tasks;
    }

    /**
     * Entry point of the runner.
     *
     * @param args .
     */
    public static void main(String... args) {
        // Note ljacqu 20151212: If the tools folder becomes a lot bigger, it will make sense to restrict the depth
        // of this recursive collector
        ClassCollector collector = new ClassCollector(TestHelper.TEST_SOURCES_FOLDER, "tools");
        Map<String, ToolTask> tasks = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (ToolTask task : collector.getInstancesOfType(ToolTask.class)) {
            tasks.put(task.getTaskName(), task);
        }

        ToolsRunner runner = new ToolsRunner(tasks);
        if (args == null || args.length == 0) {
            runner.promptAndExecuteTask();
        } else {
            runner.executeAutomaticTasks(args);
        }
    }

    private void promptAndExecuteTask() {
        System.out.println("The following tasks are available:");
        for (String key : tasks.keySet()) {
            System.out.println("- " + key);
        }

        System.out.println("Please enter the task to run:");
        Scanner scanner = new Scanner(System.in);
        String inputTask = scanner.nextLine();

        ToolTask task = tasks.get(inputTask);
        if (task != null) {
            task.execute(scanner);
        } else {
            System.out.println("Unknown task");
        }
        scanner.close();
    }

    private void executeAutomaticTasks(String... requests) {
        for (String taskName : requests) {
            ToolTask task = tasks.get(taskName);
            if (task == null) {
                System.out.format("Unknown task '%s'%n", taskName);
            } else if (task instanceof AutoToolTask) {
                ((AutoToolTask) task).executeDefault();
            } else {
                System.out.format("Task '%s' cannot be run on command line%n", taskName);
            }
        }
    }
}
