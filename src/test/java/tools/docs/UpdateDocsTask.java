package tools.docs;

import com.google.common.collect.ImmutableSet;
import tools.docs.commands.CommandPageCreater;
import tools.docs.hashmethods.HashAlgorithmsDescriptionTask;
import tools.docs.permissions.PermissionsListWriter;
import tools.docs.translations.TranslationPageGenerator;
import tools.utils.AutoToolTask;
import tools.utils.ToolTask;

import java.util.Scanner;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Task that runs all tasks which update files in the docs folder.
 */
public class UpdateDocsTask implements AutoToolTask {

    private static final Set<Class<? extends ToolTask>> TASKS = ImmutableSet
        .of(CommandPageCreater.class, HashAlgorithmsDescriptionTask.class,
            PermissionsListWriter.class, TranslationPageGenerator.class);

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

    private static ToolTask instantiateTask(Class<? extends ToolTask> clazz) {
        try {
            return clazz.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new UnsupportedOperationException("Could not instantiate task class '" + clazz + "'", e);
        }
    }

    private static void executeTasks(Consumer<ToolTask> taskRunner) {
        for (Class<? extends ToolTask> taskClass : TASKS) {
            ToolTask task = instantiateTask(taskClass);
            System.out.println("\nRunning " + task.getTaskName() + "\n-------------------");
            taskRunner.accept(task);
        }
    }
}
