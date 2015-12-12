import utils.ToolTask;
import utils.ToolsConstants;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Runner for executing tool tasks.
 */
public final class ToolsRunner {

    private ToolsRunner() {
    }

    /**
     * Entry point of the runner.
     *
     * @param args .
     */
    public static void main(String... args) {
        // Collect tasks and show them
        File toolsFolder = new File(ToolsConstants.TOOLS_SOURCE_ROOT);
        Map<String, ToolTask> tasks = new HashMap<>();
        collectTasksInDirectory(toolsFolder, tasks);
        listAllTasks(tasks);

        // Prompt user for task and handle input
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

    private static void listAllTasks(Map<String, ToolTask> taskCollection) {
        System.out.println("The following tasks are available:");
        for (String key : taskCollection.keySet()) {
            System.out.println("- " + key);
        }
    }

    /**
     * Add all implementations of {@link ToolTask} from the given folder to the provided collection.
     *
     * @param dir The directory to scan
     * @param taskCollection The collection to add results to
     */
    // Note ljacqu 20151212: If the tools folder becomes a lot bigger, it will make sense to restrict the depth
    // of this recursive collector
    private static void collectTasksInDirectory(File dir, Map<String, ToolTask> taskCollection) {
        File[] files = dir.listFiles();
        if (files == null) {
            throw new RuntimeException("Cannot read folder '" + dir + "'");
        }
        for (File file : files) {
            if (file.isDirectory()) {
                collectTasksInDirectory(file, taskCollection);
            } else if (file.isFile()) {
                ToolTask task = getTaskFromFile(file);
                if (task != null) {
                    taskCollection.put(task.getTaskName(), task);
                }
            }
        }
    }

    /**
     * Return a {@link ToolTask} instance defined by the given source file.
     *
     * @param file The file to load
     * @return ToolTask instance, or null if not applicable
     */
    private static ToolTask getTaskFromFile(File file) {
        Class<? extends ToolTask> taskClass = loadTaskClassFromFile(file);
        if (taskClass == null) {
            return null;
        }

        try {
            Constructor<? extends ToolTask> constructor = taskClass.getConstructor();
            return constructor.newInstance();
        } catch (NoSuchMethodException | InvocationTargetException |
                 IllegalAccessException | InstantiationException e) {
            throw new RuntimeException("Cannot instantiate task '" + taskClass + "'");
        }
    }

    /**
     * Return the class the file defines if it implements {@link ToolTask}.
     *
     * @return The class instance, or null if not applicable
     */
    @SuppressWarnings("unchecked")
    private static Class<? extends ToolTask> loadTaskClassFromFile(File file) {
        if (!file.getName().endsWith(".java")) {
            return null;
        }

        String filePath = file.getPath();
        String className = filePath
            .substring(ToolsConstants.TOOLS_SOURCE_ROOT.length(), filePath.length() - 5)
            .replace(File.separator, ".");
        try {
            Class<?> clazz = ClassLoader.getSystemClassLoader().loadClass(className);
            return ToolTask.class.isAssignableFrom(clazz) && isInstantiable(clazz)
                ? (Class<? extends ToolTask>) clazz
                : null;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isInstantiable(Class<?> clazz) {
        return !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers());
    }

}
