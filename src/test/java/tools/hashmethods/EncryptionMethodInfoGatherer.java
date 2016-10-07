package tools.hashmethods;

import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import com.github.authme.configme.properties.Property;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.security.crypts.EncryptionMethod;
import fr.xephi.authme.security.crypts.HexSaltedMethod;
import fr.xephi.authme.security.crypts.description.AsciiRestricted;
import fr.xephi.authme.security.crypts.description.HasSalt;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.settings.Settings;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Gathers information on {@link EncryptionMethod} implementations based on
 * the annotations in {@link fr.xephi.authme.security.crypts.description}.
 */
public class EncryptionMethodInfoGatherer {

    @SuppressWarnings("unchecked")
    private final static Set<Class<? extends Annotation>> RELEVANT_ANNOTATIONS =
        newHashSet(HasSalt.class, Recommendation.class, AsciiRestricted.class);

    private static Injector injector = createInitializer();

    private Map<HashAlgorithm, MethodDescription> descriptions;

    public EncryptionMethodInfoGatherer() {
        descriptions = new LinkedHashMap<>();
        constructDescriptions();
    }

    public Map<HashAlgorithm, MethodDescription> getDescriptions() {
        return descriptions;
    }

    private void constructDescriptions() {
        for (HashAlgorithm algorithm : HashAlgorithm.values()) {
            if (!HashAlgorithm.CUSTOM.equals(algorithm) && !algorithm.getClazz().isAnnotationPresent(Deprecated.class)) {
                MethodDescription description = createDescription(algorithm);
                descriptions.put(algorithm, description);
            }
        }
    }

    private static MethodDescription createDescription(HashAlgorithm algorithm) {
        Class<? extends EncryptionMethod> clazz = algorithm.getClazz();
        EncryptionMethod method = injector.newInstance(clazz);
        if (method == null) {
            throw new NullPointerException("Method for '" + algorithm + "' is null");
        }
        MethodDescription description = new MethodDescription(clazz);
        description.setHashLength(method.computeHash("test", "user").getHash().length());
        description.setHasSeparateSalt(method.hasSeparateSalt());

        Map<Class<?>, Annotation> annotationMap = gatherAnnotations(clazz);
        if (annotationMap.containsKey(HasSalt.class)) {
            setSaltInformation(description, returnTyped(annotationMap, HasSalt.class), method);
        }
        if (annotationMap.containsKey(Recommendation.class)) {
            description.setUsage(returnTyped(annotationMap, Recommendation.class).value());
        }
        if (annotationMap.containsKey(AsciiRestricted.class)) {
            description.setAsciiRestricted(true);
        }
        return description;
    }

    private static Map<Class<?>, Annotation> gatherAnnotations(Class<?> methodClass) {
        // Note ljacqu 20151231: The map could be Map<Class<? extends Annotation>, Annotation> and it has the constraint
        // that for a key Class<T>, the value is of type T. We write a simple "Class<?>" for brevity.
        Map<Class<?>, Annotation> collection = new HashMap<>();
        Class<?> currentMethodClass = methodClass;
        while (currentMethodClass != null) {
            getRelevantAnnotations(currentMethodClass, collection);
            currentMethodClass = getSuperClass(currentMethodClass);
        }
        return collection;
    }

    // Parameters could be Class<? extends EncryptionMethod>; Map<Class<? extends Annotation>, Annotation>
    // but the constraint doesn't have any technical relevance, so just clutters the code
    private static void getRelevantAnnotations(Class<?> methodClass, Map<Class<?>, Annotation> collection) {
        for (Annotation annotation : methodClass.getAnnotations()) {
            if (RELEVANT_ANNOTATIONS.contains(annotation.annotationType())
                && !collection.containsKey(annotation.annotationType())) {
                collection.put(annotation.annotationType(), annotation);
            }
        }
    }

    /**
     * Returns the super class of the given encryption method if it is also of EncryptionMethod type.
     * (Anything beyond EncryptionMethod is not of interest.)
     */
    private static Class<?> getSuperClass(Class<?> methodClass) {
        Class<?> zuper = methodClass.getSuperclass();
        if (EncryptionMethod.class.isAssignableFrom(zuper)) {
            return zuper;
        }
        return null;
    }

    /**
     * Set the salt information for the given encryption method and the found {@link HasSalt} annotation.
     * Also gets the salt length from {@link HexSaltedMethod#getSaltLength()} for such instances.
     *
     * @param description The description to update
     * @param hasSalt     The associated HasSalt annotation
     * @param method      The encryption method
     */
    private static void setSaltInformation(MethodDescription description, HasSalt hasSalt, EncryptionMethod method) {
        description.setSaltType(hasSalt.value());
        if (hasSalt.length() != 0) {
            description.setSaltLength(hasSalt.length());
        } else if (method instanceof HexSaltedMethod) {
            int saltLength = ((HexSaltedMethod) method).getSaltLength();
            description.setSaltLength(saltLength);
        }
    }

    // Convenience method for retrieving an annotation in a typed fashion.
    // We know implicitly that the key of the map always corresponds to the type of the value
    private static <T> T returnTyped(Map<Class<?>, Annotation> map, Class<T> key) {
        return key.cast(map.get(key));
    }

    @SuppressWarnings("unchecked")
    private static Injector createInitializer() {
        Settings settings = mock(Settings.class);
        // Return the default value for any property
        when(settings.getProperty(any(Property.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Property<?> property = (Property<?>) invocation.getArguments()[0];
                return property.getDefaultValue();
            }
        });

        // By passing some bogus "package" to the constructor, the injector will throw if it needs to
        // instantiate any dependency other than what we provide.
        Injector injector = new InjectorBuilder().addDefaultHandlers("fr.xephi.authme.security.crypts").create();
        injector.register(Settings.class, settings);
        return injector;
    }

}
