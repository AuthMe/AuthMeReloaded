package hashmethods;

import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.security.crypts.EncryptionMethod;
import fr.xephi.authme.security.crypts.HexSaltedMethod;
import fr.xephi.authme.security.crypts.description.AsciiRestricted;
import fr.xephi.authme.security.crypts.description.HasSalt;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.mockito.BDDMockito;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.mockito.Mockito.mock;

/**
 * Gathers information on {@link EncryptionMethod} implementations based on
 * the annotations in {@link fr.xephi.authme.security.crypts.description}.
 */
public class EncryptionMethodInfoGatherer {

    @SuppressWarnings("unchecked")
    private final static Set<Class<? extends Annotation>> RELEVANT_ANNOTATIONS =
        newHashSet(HasSalt.class, Recommendation.class, AsciiRestricted.class);

    private static NewSetting settings = createSettings();

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
        EncryptionMethod method = null; // TODO ljacqu PasswordSecurity.initializeEncryptionMethod(algorithm, settings);
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

    private static NewSetting createSettings() {
        // TODO #672 Don't mock settings but instantiate a NewSetting object without any validation / migration
        NewSetting settings = mock(NewSetting.class);
        BDDMockito.given(settings.getProperty(HooksSettings.BCRYPT_LOG2_ROUND)).willReturn(8);
        BDDMockito.given(settings.getProperty(SecuritySettings.DOUBLE_MD5_SALT_LENGTH)).willReturn(8);
        return settings;

        /*try (InputStreamReader isr = new InputStreamReader(getClass().getResourceAsStream("config.yml"))) {
            FileConfiguration configuration = YamlConfiguration.loadConfiguration(isr);
            return new NewSetting(configuration, null, null, null);
        } catch (IOException e) {
            throw new UnsupportedOperationException(e);
        }*/
    }

}
