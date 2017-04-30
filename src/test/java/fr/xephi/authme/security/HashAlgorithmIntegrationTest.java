package fr.xephi.authme.security;

import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import fr.xephi.authme.security.crypts.EncryptionMethod;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Integration test for {@link HashAlgorithm}.
 */
public class HashAlgorithmIntegrationTest {

    private static Injector injector;

    @BeforeClass
    public static void setUpConfigAndInjector() {
        Settings settings = mock(Settings.class);
        given(settings.getProperty(HooksSettings.BCRYPT_LOG2_ROUND)).willReturn(8);
        given(settings.getProperty(SecuritySettings.DOUBLE_MD5_SALT_LENGTH)).willReturn(16);
        given(settings.getProperty(SecuritySettings.PBKDF2_NUMBER_OF_ROUNDS)).willReturn(10_000);
        injector = new InjectorBuilder().addDefaultHandlers("fr.xephi.authme").create();
        injector.register(Settings.class, settings);
    }

    @Test
    public void shouldHaveUniqueClassForEntries() {
        // given
        Set<Class<? extends EncryptionMethod>> classes = new HashSet<>();

        // when / then
        for (HashAlgorithm algorithm : HashAlgorithm.values()) {
            if (!HashAlgorithm.CUSTOM.equals(algorithm)) {
                if (!classes.add(algorithm.getClazz())) {
                    fail("Found class '" + algorithm.getClazz() + "' twice!");
                }
            }
        }
    }

    @Test
    public void shouldBeAbleToInstantiateEncryptionAlgorithms() {
        // given / when / then
        for (HashAlgorithm algorithm : HashAlgorithm.values()) {
            if (!HashAlgorithm.CUSTOM.equals(algorithm) && !HashAlgorithm.PLAINTEXT.equals(algorithm)) {
                EncryptionMethod method = injector.createIfHasDependencies(algorithm.getClazz());
                if (method == null) {
                    fail("Could not create '" + algorithm.getClazz() + "' - forgot to provide some class?");
                }
                HashedPassword hashedPassword = method.computeHash("pwd", "name");
                assertThat("Salt should not be null if method.hasSeparateSalt(), and vice versa. Method: '"
                    + method + "'", StringUtils.isEmpty(hashedPassword.getSalt()), equalTo(!method.hasSeparateSalt()));
                assertThat("Hash should not be empty for method '" + method + "'",
                    StringUtils.isEmpty(hashedPassword.getHash()), equalTo(false));
            }
        }
    }

}
