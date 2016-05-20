package tools.docs;

import com.google.common.collect.ImmutableSet;
import tools.commands.CommandPageCreater;
import tools.hashmethods.HashAlgorithmsDescriptionTask;
import tools.permissions.PermissionsListWriter;
import tools.utils.AutoToolTask;
import tools.utils.ToolTask;

import java.util.Scanner;
import java.util.Set;

/**
 * Task that runs all tasks which update files in the docs folder.
 */
public class UpdateDocsTask implements AutoToolTask {

    private static final Set<Class<? extends ToolTask>> TASKS = ImmutableSet.<Class<? extends ToolTask>>of(
        CommandPageCreater.class, HashAlgorithmsDescriptionTask.class, PermissionsListWriter.class);

    @Override
    public String getTaskName() {
        return "updateDocs";
    }

    @Override
    public void execute(final Scanner scanner) {
        executeTasks(new TaskRunner() {
            @Override
            public void execute(ToolTask task) {
                task.execute(scanner);
            }
        });
    }

    @Override
    public void executeDefault() {
        executeTasks(new TaskRunner() {
            @Override
            public void execute(ToolTask task) {
                if (task instanceof AutoToolTask) {
                    ((AutoToolTask) task).executeDefault();
                }
            }
        });
    }

    private static ToolTask instantiateTask(Class<? extends ToolTask> clazz) {
        try {
            return clazz.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    private static void executeTasks(TaskRunner runner) {
        for (Class<? extends ToolTask> taskClass : TASKS) {
            try {
                ToolTask task = instantiateTask(taskClass);
                System.out.println("\nRunning " + task.getTaskName() + "\n-------------------");
                runner.execute(task);
            } catch (UnsupportedOperationException e) {
                System.err.println("Error running task of class '" + taskClass + "'");
                e.printStackTrace();
            }
        }
    }

    private interface TaskRunner {
        void execute(ToolTask task);
    }
}
