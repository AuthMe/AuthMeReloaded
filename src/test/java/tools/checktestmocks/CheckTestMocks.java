package tools.checktestmocks;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import fr.xephi.authme.util.StringUtils;
import org.mockito.Mock;
import tools.utils.AutoToolTask;
import tools.utils.InjectorUtils;
import tools.utils.ToolsConstants;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * Task checking if all tests' {@code @Mock} fields have a corresponding
 * {@code @Inject} field in the class they are testing.
 */
public class CheckTestMocks implements AutoToolTask {

    private List<String> errors = new ArrayList<>();

    @Override
    public String getTaskName() {
        return "checkTestMocks";
    }

    @Override
    public void execute(Scanner scanner) {
        executeDefault();
    }

    @Override
    public void executeDefault() {
        readAndCheckFiles(new File(ToolsConstants.TEST_SOURCE_ROOT));
        System.out.println(StringUtils.join("\n", errors));
    }

    /**
     * Recursively reads directories and checks the contained classes.
     *
     * @param dir the directory to read
     */
    private void readAndCheckFiles(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            throw new IllegalStateException("Cannot read folder '" + dir + "'");
        }
        for (File file : files) {
            if (file.isDirectory()) {
                readAndCheckFiles(file);
            } else if (file.isFile()) {
                Class<?> clazz = loadTestClass(file);
                if (clazz != null) {
                    checkClass(clazz);
                }
                // else System.out.format("No @Mock fields found in class of file '%s'%n", file.getName())
            }
        }
    }

    /**
     * Checks the given test class' @Mock fields against the corresponding production class' @Inject fields.
     *
     * @param testClass the test class to verify
     */
    private void checkClass(Class<?> testClass) {
        Class<?> realClass = returnRealClass(testClass);
        if (realClass != null) {
            Set<Class<?>> mockFields = getMocks(testClass);
            Set<Class<?>> injectFields = InjectorUtils.getDependencies(realClass);
            if (injectFields == null) {
                addErrorEntry(testClass, "Could not find instantiation method");
            } else if (!injectFields.containsAll(mockFields)) {
                addErrorEntry(testClass, "Error - Found the following mocks absent as @Inject: "
                    + formatClassList(Sets.difference(mockFields, injectFields)));
            } else if (!mockFields.containsAll(injectFields)) {
                addErrorEntry(testClass, "Info - Found @Inject fields which are not present as @Mock: "
                    + formatClassList(Sets.difference(injectFields, mockFields)));
            }
        }
    }

    private void addErrorEntry(Class<?> clazz, String message) {
        errors.add(clazz.getSimpleName() + ": " + message);
    }

    private static Class<?> loadTestClass(File file) {
        String fileName = file.getPath();
        String className = fileName
            // Strip source folders and .java ending
            .substring("src/test/java/".length(), fileName.length() - 5)
            .replace(File.separator, ".");
        try {
            Class<?> clazz = CheckTestMocks.class.getClassLoader().loadClass(className);
            return isTestClassWithMocks(clazz) ? clazz : null;
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    private static Set<Class<?>> getMocks(Class<?> clazz) {
        Set<Class<?>> result = new HashSet<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Mock.class)) {
                result.add(field.getType());
            }
        }
        return result;
    }

    /**
     * Returns the production class ("real class") corresponding to the given test class.
     * Returns null if the production class could not be mapped or loaded.
     *
     * @param testClass the test class to find the corresponding production class for
     * @return production class, or null if not found
     */
    private static Class<?> returnRealClass(Class<?> testClass) {
        String testClassName = testClass.getName();
        String realClassName = testClassName.replaceAll("(Integration|Consistency)?Test$", "");
        if (realClassName.equals(testClassName)) {
            System.out.format("%s doesn't match typical test class naming pattern.%n", testClassName);
            return null;
        }
        try {
            return CheckTestMocks.class.getClassLoader().loadClass(realClassName);
        } catch (ClassNotFoundException e) {
            System.out.format("Real class '%s' not found for test class '%s'%n", realClassName, testClassName);
            return null;
        }
    }

    private static boolean isTestClassWithMocks(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Mock.class)) {
                return true;
            }
        }
        return false;
    }

    private static String formatClassList(Collection<Class<?>> coll) {
        Collection<String> classNames = Collections2.transform(coll, new Function<Class<?>, String>() {
            @Override
            public String apply(Class<?> input) {
                return input.getSimpleName();
            }
        });
        return StringUtils.join(", ", classNames);
    }

}
