package fr.xephi.authme.security;

import fr.xephi.authme.initialization.AuthMeServiceInitializer;
import fr.xephi.authme.security.crypts.EncryptionMethod;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.NewSetting;
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

    private static AuthMeServiceInitializer initializer;

    @BeforeClass
    public static void setUpConfigAndInjector() {
        NewSetting settings = mock(NewSetting.class);
        given(settings.getProperty(HooksSettings.BCRYPT_LOG2_ROUND)).willReturn(8);
        given(settings.getProperty(SecuritySettings.DOUBLE_MD5_SALT_LENGTH)).willReturn(16);
        initializer = new AuthMeServiceInitializer();
        initializer.register(NewSetting.class, settings);
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
                EncryptionMethod method = initializer.newInstance(algorithm.getClazz());
                HashedPassword hashedPassword = method.computeHash("pwd", "name");
                assertThat("Salt should not be null if method.hasSeparateSalt(), and vice versa. Method: '"
                    + method + "'", StringUtils.isEmpty(hashedPassword.getSalt()), equalTo(!method.hasSeparateSalt()));
                assertThat("Hash should not be empty for method '" + method + "'",
                    StringUtils.isEmpty(hashedPassword.getHash()), equalTo(false));
            }
        }
    }

}
