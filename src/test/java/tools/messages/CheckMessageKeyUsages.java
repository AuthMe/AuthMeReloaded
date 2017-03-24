package tools.messages;

import com.google.common.collect.Lists;
import fr.xephi.authme.message.MessageKey;
import tools.utils.FileIoUtils;
import tools.utils.ToolTask;
import tools.utils.ToolsConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

/**
 * Task which checks for {@link MessageKey} usages.
 */
public class CheckMessageKeyUsages implements ToolTask {

    private static final Predicate<File> SHOULD_CHECK_FILE =
        file -> file.getName().endsWith(".java") && !file.getName().endsWith("MessageKey.java");

    @Override
    public String getTaskName() {
        return "checkMessageUses";
    }

    @Override
    public void execute(Scanner scanner) {
        System.out.println("Enter a message key to find the files where it is used");
        System.out.println("Enter empty line to search for all unused message keys");
        String key = scanner.nextLine();

        if (key.trim().isEmpty()) {
            List<MessageKey> unusedKeys = findUnusedKeys();
            if (unusedKeys.isEmpty()) {
                System.out.println("No unused MessageKey entries found :)");
            } else {
                System.out.println("Did not find usages for keys:\n- " +
                    String.join("\n- ", Lists.transform(unusedKeys, MessageKey::name)));
            }
        } else {
            MessageKey messageKey = MessageKey.valueOf(key);
            List<File> filesUsingKey = findUsagesOfKey(messageKey);
            System.out.println("The following files use '" + key + "':\n- "
                + filesUsingKey.stream().map(File::getName).collect(Collectors.joining("\n- ")));
        }
    }

    private List<MessageKey> findUnusedKeys() {
        List<MessageKey> keys = new ArrayList<>(asList(MessageKey.values()));
        File sourceFolder = new File(ToolsConstants.MAIN_SOURCE_ROOT);

        Consumer<File> fileProcessor = file -> {
            String source = FileIoUtils.readFromFile(file.toPath());
            keys.removeIf(key -> source.contains(key.name()));
        };

        walkJavaFileTree(sourceFolder, fileProcessor);
        return keys;
    }

    private List<File> findUsagesOfKey(MessageKey key) {
        List<File> filesUsingKey = new ArrayList<>();
        File sourceFolder = new File(ToolsConstants.MAIN_SOURCE_ROOT);

        Consumer<File> usagesCollector = file -> {
            String source = FileIoUtils.readFromFile(file.toPath());
            if (source.contains(key.name())) {
                filesUsingKey.add(file);
            }
        };

        walkJavaFileTree(sourceFolder, usagesCollector);
        return filesUsingKey;
    }

    private static void walkJavaFileTree(File folder, Consumer<File> javaFileConsumer) {
        for (File file : FileIoUtils.listFilesOrThrow(folder)) {
            if (file.isDirectory()) {
                walkJavaFileTree(file, javaFileConsumer);
            } else if (file.isFile() && SHOULD_CHECK_FILE.test(file)) {
                javaFileConsumer.accept(file);
            }
        }
    }
}
