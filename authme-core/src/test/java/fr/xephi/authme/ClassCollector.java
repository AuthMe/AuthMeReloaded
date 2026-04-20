package fr.xephi.authme;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Collects available classes by walking through a source directory.
 * <p>
 * This is a naive, zero dependency collector that walks through a file directory
 * and loads classes from the class loader based on the .java files it encounters.
 * This is a very slow approach and should be avoided for production code.
 * <p>
 * For more performant approaches, see e.g. <a href="https://github.com/ronmamo/reflections">org.reflections</a>.
 */
public class ClassCollector {

    private final String root;
    private final String nonCodePath;

    /**
     * Constructor. The arguments make up the path from which the collector will start scanning.
     *
     * @param nonCodePath beginning of the starting path that are not Java packages, e.g. {@code src/main/java/}
     * @param packagePath folders following {@code nonCodePath} that are packages, e.g. {@code com/project/app}
     */
    public ClassCollector(String nonCodePath, String packagePath) {
        if (!nonCodePath.endsWith("/") && !nonCodePath.endsWith("\\")) {
            nonCodePath = nonCodePath.concat(File.separator);
        }
        this.root = nonCodePath + packagePath;
        this.nonCodePath = nonCodePath;
    }

    /**
     * Collects all classes from the parent folder and below.
     *
     * @return all classes
     */
    public List<Class<?>> collectClasses() {
        return collectClasses(x -> true);
    }

    /**
     * Collects all classes from the parent folder and below which are of type {@link T}.
     *
     * @param parent the parent which classes need to extend (or be equal to) in order to be collected
     * @param <T> the parent type
     * @return list of matching classes
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> List<Class<? extends T>> collectClasses(Class<T> parent) {
        List<Class<?>> classes = collectClasses(parent::isAssignableFrom);
        return new ArrayList<>((List) classes);
    }

    /**
     * Collects all classes from the parent folder and below which match the given predicate.
     *
     * @param filter the predicate classes need to satisfy in order to be collected
     * @return list of matching classes
     */
    public List<Class<?>> collectClasses(Predicate<Class<?>> filter) {
        File rootFolder = new File(root);
        List<Class<?>> collection = new ArrayList<>();
        gatherClassesFromFile(rootFolder, filter, collection);
        return collection;
    }

    /**
     * Constructs an instance of all classes which are of the provided type {@code clazz}.
     * This method assumes that every class has an accessible no-args constructor for creation.
     *
     * @param parent the parent which classes need to extend (or be equal to) in order to be instantiated
     * @param <T> the parent type
     * @return collection of created objects
     */
    public <T> List<T> getInstancesOfType(Class<T> parent) {
        return getInstancesOfType(parent, (clz) -> {
           try {
               return canInstantiate(clz) ? clz.newInstance() : null;
           } catch (InstantiationException | IllegalAccessException e) {
               throw new IllegalStateException(e);
           }
        });
    }

    /**
     * Constructs an instance of all classes which are of the provided type {@code clazz}
     * with the provided {@code instantiator}.
     *
     * @param parent the parent which classes need to extend (or be equal to) in order to be instantiated
     * @param instantiator function which returns an object of the given class, or null to skip the class
     * @param <T> the parent type
     * @return collection of created objects
     */
    public <T> List<T> getInstancesOfType(Class<T> parent, Function<Class<? extends T>, T> instantiator) {
        return collectClasses(parent)
            .stream()
            .map(instantiator)
            .filter(o -> o != null)
            .collect(Collectors.toList());
    }

    /**
     * Returns whether the given class can be instantiated, i.e. if it is not abstract, an interface, etc.
     *
     * @param clazz the class to process
     * @return true if the class can be instantiated, false otherwise
     */
    public static boolean canInstantiate(Class<?> clazz) {
        return clazz != null && !clazz.isEnum() && !clazz.isInterface()
            && !clazz.isArray() && !Modifier.isAbstract(clazz.getModifiers());
    }

    /**
     * Recursively collects the classes based on the files in the directory and in its child directories.
     *
     * @param folder the folder to scan
     * @param filter the class predicate
     * @param collection collection to add classes to
     */
    private void gatherClassesFromFile(File folder, Predicate<Class<?>> filter, List<Class<?>> collection) {
        File[] files = folder.listFiles();
        if (files == null) {
            throw new IllegalStateException("Could not read files from '" + folder + "'");
        }
        for (File file : files) {
            if (file.isDirectory()) {
                gatherClassesFromFile(file, filter, collection);
            } else if (file.isFile()) {
                Class<?> clazz = loadTaskClassFromFile(file);
                if (clazz != null && filter.test(clazz)) {
                    collection.add(clazz);
                }
            }
        }
    }

    /**
     * Loads a class from the class loader based on the given file.
     *
     * @param file the file whose corresponding Java class should be retrieved
     * @return the corresponding class, or null if not applicable
     */
    private Class<?> loadTaskClassFromFile(File file) {
        if (!file.getName().endsWith(".java")) {
            return null;
        }

        String filePath = file.getPath();
        String className = filePath
            .substring(nonCodePath.length(), filePath.length() - 5)
            .replace(File.separator, ".");
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
