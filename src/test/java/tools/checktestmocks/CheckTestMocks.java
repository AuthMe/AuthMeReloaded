package tools.checktestmocks;

import com.google.common.collect.Sets;
import fr.xephi.authme.ClassCollector;
import fr.xephi.authme.TestHelper;
import org.mockito.Mock;
import tools.utils.AutoToolTask;
import tools.utils.InjectorUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    public void executeDefault() {
        ClassCollector collector = new ClassCollector(TestHelper.TEST_SOURCES_FOLDER, TestHelper.PROJECT_ROOT);
        for (Class<?> clazz : collector.collectClasses(c -> isTestClassWithMocks(c))) {
            checkClass(clazz);
        }
        System.out.println(String.join("\n", errors));
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
        return coll.stream()
            .map(Class::getSimpleName)
            .collect(Collectors.joining(", "));
    }

}
