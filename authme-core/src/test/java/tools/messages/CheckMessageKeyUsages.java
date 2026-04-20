package tools.messages;

import com.google.common.collect.Lists;
import fr.xephi.authme.message.MessageKey;
import tools.utils.AutoToolTask;
import tools.utils.FileIoUtils;
import tools.utils.ToolsConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.util.Arrays.asList;

/**
 * Task which checks for {@link MessageKey} usages.
 */
public class CheckMessageKeyUsages implements AutoToolTask {

    private static final Predicate<File> SHOULD_CHECK_FILE =
        file -> file.getName().endsWith(".java") && !file.getName().endsWith("MessageKey.java");

    @Override
    public String getTaskName() {
        return "checkMessageUses";
    }

    @Override
    public void executeDefault() {
        List<MessageKey> unusedKeys = findUnusedKeys();
        if (unusedKeys.isEmpty()) {
            System.out.println("No unused MessageKey entries found :)");
        } else {
            System.out.println("Did not find usages for keys:\n- "
                + String.join("\n- ", Lists.transform(unusedKeys, MessageKey::name)));
        }
    }

    private List<MessageKey> findUnusedKeys() {
        List<MessageKey> keys = new ArrayList<>(asList(MessageKey.values()));
        File sourceFolder = new File(ToolsConstants.MAIN_SOURCE_ROOT);

        Consumer<File> fileProcessor = file -> {
            String source = FileIoUtils.readFromFile(file.toPath());
            keys.removeIf(key -> source.contains("MessageKey." + key.name()));
        };

        walkJavaFileTree(sourceFolder, fileProcessor);
        return keys;
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
