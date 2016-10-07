package tools.dependencygraph;

import ch.jalu.injector.handlers.instantiation.DependencyDescription;
import ch.jalu.injector.handlers.instantiation.Instantiation;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import fr.xephi.authme.ClassCollector;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.datasource.converter.Converter;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.process.AsynchronousProcess;
import fr.xephi.authme.process.SynchronousProcess;
import fr.xephi.authme.security.crypts.EncryptionMethod;
import org.bukkit.event.Listener;
import tools.utils.InjectorUtils;
import tools.utils.ToolTask;
import tools.utils.ToolsConstants;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Creates a DOT file of the dependencies in AuthMe classes.
 */
public class DrawDependency implements ToolTask {

    private static final String DOT_FILE = ToolsConstants.TOOLS_SOURCE_ROOT + "dependencygraph/graph.dot";

    private static final List<Class<?>> SUPER_TYPES = ImmutableList.of(ExecutableCommand.class,
        SynchronousProcess.class, AsynchronousProcess.class, EncryptionMethod.class, Converter.class, Listener.class);

    /** Annotation types by which dependencies are identified. */
    private static final List<Class<? extends Annotation>> ANNOTATION_TYPES = ImmutableList.of(DataFolder.class);

    private boolean mapToSupertype;
    // Map with the graph's nodes: value is one of the key's dependencies
    private Multimap<Class<?>, String> foundDependencies = HashMultimap.create();

    @Override
    public String getTaskName() {
        return "drawDependencyGraph";
    }

    @Override
    public void execute(Scanner scanner) {
        System.out.println("Summarize classes to their generic super type where applicable?");
        mapToSupertype = "y".equalsIgnoreCase(scanner.nextLine());

        // Gather all connections
        ClassCollector collector = new ClassCollector(TestHelper.SOURCES_FOLDER, TestHelper.PROJECT_ROOT);
        for (Class<?> clazz : collector.collectClasses()) {
            processClass(clazz);
        }

        // Prompt user for simplification of graph
        System.out.println("Do you want to remove classes that are not used as dependency elsewhere?");
        System.out.println("Specify the number of times to do this: [0=keep all]");
        int stripVerticesCount;
        try {
            stripVerticesCount = Integer.valueOf(scanner.nextLine());
        } catch (NumberFormatException e) {
            stripVerticesCount = 0;
        }

        // Perform simplification as per user's wish
        for (int i = 0; i < stripVerticesCount; ++i) {
            stripNodesWithNoOutgoingEdges();
        }

        // Create dot file content
        final String pattern = "\t\"%s\" -> \"%s\";";
        String dotFile = "";
        for (Map.Entry<Class<?>, String> entry : foundDependencies.entries()) {
            dotFile += "\n" + String.format(pattern, entry.getValue(), entry.getKey().getSimpleName());
        }

        // Write dot file
        try {
            Files.write(Paths.get(DOT_FILE), ("digraph G {\n" + dotFile + "\n}").getBytes());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        System.out.println("Graph file written");
        System.out.format("Run 'dot -Tpng %s -o graph.png' to generate image (requires GraphViz)%n", DOT_FILE);
    }

    private void processClass(Class<?> clazz) {
        List<String> dependencies = getDependencies(clazz);
        if (dependencies != null) {
            foundDependencies.putAll(mapToSuper(clazz), dependencies);
        }
    }

    private Class<?> mapToSuper(Class<?> clazz) {
        if (!mapToSupertype || clazz == null) {
            return clazz;
        }
        for (Class<?> parent : SUPER_TYPES) {
            if (parent.isAssignableFrom(clazz)) {
                return parent;
            }
        }
        return clazz;
    }

    private List<String> getDependencies(Class<?> clazz) {
        Instantiation<?> instantiation = InjectorUtils.getInstantiationMethod(clazz);
        return instantiation == null ? null : formatInjectionDependencies(instantiation);
    }

    /**
     * Formats the dependencies returned by the given injection appropriately:
     * if the dependency has an annotation, the annotation will be returned;
     * otherwise the class name.
     *
     * @param injection the injection whose dependencies should be formatted
     * @return list of dependencies in a friendly format
     */
    private List<String> formatInjectionDependencies(Instantiation<?> injection) {
        List<? extends DependencyDescription> descriptions = injection.getDependencies();
        final int totalDependencies = descriptions.size();
        Class<?>[] dependencies = new Class<?>[totalDependencies];
        Class<?>[] annotations = new Class<?>[totalDependencies];
        for (int i = 0; i < descriptions.size(); ++i) {
            dependencies[i] = descriptions.get(i).getType();
            annotations[i] = getRelevantAnnotationClass(descriptions.get(i).getAnnotations());
        }

        List<String> result = new ArrayList<>(dependencies.length);
        for (int i = 0; i < dependencies.length; ++i) {
            if (annotations[i] != null) {
                result.add("@" + annotations[i].getSimpleName());
            } else {
                result.add(mapToSuper(dependencies[i]).getSimpleName());
            }
        }
        return result;
    }

    private static Class<? extends Annotation> getRelevantAnnotationClass(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (ANNOTATION_TYPES.contains(annotation.annotationType())) {
                return annotation.annotationType();
            }
        }
        return null;
    }

    /**
     * Removes all vertices in the graph that have no outgoing edges, i.e. all classes
     * in the graph that only receive dependencies but are not used as a dependency anywhere.
     * This process can be repeated multiple times.
     */
    private void stripNodesWithNoOutgoingEdges() {
        Map<String, Boolean> dependencies = new HashMap<>();
        for (Map.Entry<Class<?>, String> entry : foundDependencies.entries()) {
            final String className = entry.getKey().getSimpleName();
            dependencies.put(className, Boolean.FALSE);
            dependencies.put(entry.getValue(), Boolean.TRUE);
        }

        Iterator<Class<?>> it = foundDependencies.keys().iterator();
        while (it.hasNext()) {
            Class<?> clazz = it.next();
            if (Boolean.FALSE.equals(dependencies.get(clazz.getSimpleName()))) {
                it.remove();
            }
        }
    }
}
