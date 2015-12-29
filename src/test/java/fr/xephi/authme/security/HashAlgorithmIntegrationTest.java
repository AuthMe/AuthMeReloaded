package fr.xephi.authme.security;

import fr.xephi.authme.security.crypts.EncryptionMethod;
import fr.xephi.authme.security.crypts.HashResult;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.StringUtils;
import fr.xephi.authme.util.WrapperMock;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Integration test for {@link HashAlgorithm}.
 */
public class HashAlgorithmIntegrationTest {

    @BeforeClass
    public static void setUpWrapper() {
        WrapperMock.createInstance();
        Settings.bCryptLog2Rounds = 8;
        Settings.saltLength = 16;
    }

    @Test
    public void shouldHaveUniqueClassForEntries() {
        // given
        Set<Class<? extends EncryptionMethod>> classes = new HashSet<>();

        // when / then
        for (HashAlgorithm algorithm : HashAlgorithm.values()) {
            if (!HashAlgorithm.CUSTOM.equals(algorithm)) {
                if (classes.contains(algorithm.getClazz())) {
                    fail("Found class '" + algorithm.getClazz() + "' twice!");
                }
                classes.add(algorithm.getClazz());
            }
        }
    }

    @Test
    public void shouldBeAbleToInstantiateEncryptionAlgorithms() throws InstantiationException, IllegalAccessException {
        // given / when / then
        for (HashAlgorithm algorithm : HashAlgorithm.values()) {
            if (!HashAlgorithm.CUSTOM.equals(algorithm)) {
                EncryptionMethod method = algorithm.getClazz().newInstance();
                HashResult hashResult = method.computeHash("pwd", "name");
                assertThat("Salt should not be null if method.hasSeparateSalt(), and vice versa. Method: '"
                    + method + "'", StringUtils.isEmpty(hashResult.getSalt()), equalTo(!method.hasSeparateSalt()));
                assertThat("Hash should not be empty for method '" + method + "'",
                    StringUtils.isEmpty(hashResult.getHash()), equalTo(false));
            }
        }
    }

}
