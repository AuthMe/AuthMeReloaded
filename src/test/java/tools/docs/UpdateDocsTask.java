package tools.docs;

import fr.xephi.authme.ClassCollector;
import fr.xephi.authme.TestHelper;
import tools.utils.AutoToolTask;
import tools.utils.ToolTask;

import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Task that runs all tasks which update files in the docs folder.
 */
public class UpdateDocsTask implements AutoToolTask {

    @Override
    public String getTaskName() {
        return "updateDocs";
    }

    @Override
    public void execute(final Scanner scanner) {
        executeTasks(task -> task.execute(scanner));
    }

    @Override
    public void executeDefault() {
        executeTasks(task -> {
            if (task instanceof AutoToolTask) {
                ((AutoToolTask) task).executeDefault();
            }
        });
    }

    private void executeTasks(Consumer<ToolTask> taskRunner) {
        for (ToolTask task : getDocTasks()) {
            System.out.println("\nRunning " + task.getTaskName() + "\n-------------------");
            taskRunner.accept(task);
        }
    }

    private List<ToolTask> getDocTasks() {
        ClassCollector classCollector =
            new ClassCollector(TestHelper.TEST_SOURCES_FOLDER, "tools/docs");
        return classCollector.getInstancesOfType(ToolTask.class).stream()
            .filter(task -> task.getClass() != getClass())
            .collect(Collectors.toList());
    }
}
