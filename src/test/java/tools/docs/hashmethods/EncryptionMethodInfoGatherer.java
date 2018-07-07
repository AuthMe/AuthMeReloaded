package tools.docs.hashmethods;

import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import com.google.common.collect.ImmutableSet;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.security.crypts.Argon2;
import fr.xephi.authme.security.crypts.EncryptionMethod;
import fr.xephi.authme.security.crypts.HexSaltedMethod;
import fr.xephi.authme.security.crypts.description.AsciiRestricted;
import fr.xephi.authme.security.crypts.description.HasSalt;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.settings.Settings;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static org.mockito.Mockito.mock;

/**
 * Gathers information on {@link EncryptionMethod} implementations based on
 * the annotations in {@link fr.xephi.authme.security.crypts.description}.
 */
public class EncryptionMethodInfoGatherer {

    private static final Set<Class<? extends Annotation>> RELEVANT_ANNOTATIONS =
        ImmutableSet.of(HasSalt.class, Recommendation.class, AsciiRestricted.class);

    private static Injector injector = createInitializer();

    private Map<HashAlgorithm, MethodDescription> descriptions;

    public EncryptionMethodInfoGatherer() {
        ConsoleLogger.setLogger(Logger.getAnonymousLogger()); // set logger because of Argon2.isLibraryLoaded()
        descriptions = new LinkedHashMap<>();
        constructDescriptions();
    }

    public Map<HashAlgorithm, MethodDescription> getDescriptions() {
        return descriptions;
    }

    private void constructDescriptions() {
        for (HashAlgorithm algorithm : HashAlgorithm.values()) {
            if (algorithm.getClazz() != null && !algorithm.getClazz().isAnnotationPresent(Deprecated.class)) {
                MethodDescription description = createDescription(algorithm);
                descriptions.put(algorithm, description);
            }
        }
    }

    /**
     * Creates a description of the given hash algorithm based on its annotations.
     *
     * @param algorithm the algorithm to describe
     * @return description of the hash algorithm
     */
    private static MethodDescription createDescription(HashAlgorithm algorithm) {
        Class<? extends EncryptionMethod> clazz = algorithm.getClazz();
        EncryptionMethod method = createEncryptionMethod(clazz);
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

    private static EncryptionMethod createEncryptionMethod(Class<? extends EncryptionMethod> clazz) {
        if (clazz == Argon2.class && !Argon2.isLibraryLoaded()) {
            // The library for Argon2 isn't installed, so override the hash implementation to avoid using the library
            return new Argon2DummyExtension();
        }

        EncryptionMethod method = injector.createIfHasDependencies(clazz);
        if (method == null) {
            throw new NullPointerException("Failed to instantiate '" + clazz + "'. Is a dependency missing?");
        }
        return method;
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
     *
     * @param methodClass the class to process
     * @return the super class of the given class if it is also an EncryptionMethod type, otherwise null
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

    private static Injector createInitializer() {
        Settings settings = mock(Settings.class);
        TestHelper.returnDefaultsForAllProperties(settings);

        // Limit instantiation to the crypts package only so any dependencies need to be passed explicitly
        Injector injector = new InjectorBuilder().addDefaultHandlers("fr.xephi.authme.security.crypts").create();
        injector.register(Settings.class, settings);
        return injector;
    }

    private static final class Argon2DummyExtension extends Argon2 {
        @Override
        public String computeHash(String password) {
            // Argon2 produces hashes of 96 characters -> return dummy value with this length
            return String.join("", Collections.nCopies(96, "."));
        }
    }
}
