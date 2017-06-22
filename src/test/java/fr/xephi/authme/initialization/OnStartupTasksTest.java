package fr.xephi.authme.initialization;

import fr.xephi.authme.security.HashAlgorithm;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link OnStartupTasks}.
 */
public class OnStartupTasksTest {

    @Test
    public void shouldCheckIfHashIsDeprecatedIn54() {
        // given / when / then
        assertThat(OnStartupTasks.isHashDeprecatedIn54(HashAlgorithm.CUSTOM), equalTo(false));
        assertThat(OnStartupTasks.isHashDeprecatedIn54(HashAlgorithm.IPB3), equalTo(false));
        assertThat(OnStartupTasks.isHashDeprecatedIn54(HashAlgorithm.PLAINTEXT), equalTo(false));
        assertThat(OnStartupTasks.isHashDeprecatedIn54(HashAlgorithm.SHA256), equalTo(false));
        assertThat(OnStartupTasks.isHashDeprecatedIn54(HashAlgorithm.WORDPRESS), equalTo(false));

        assertThat(OnStartupTasks.isHashDeprecatedIn54(HashAlgorithm.MD5), equalTo(true));
        assertThat(OnStartupTasks.isHashDeprecatedIn54(HashAlgorithm.SHA512), equalTo(true));
        assertThat(OnStartupTasks.isHashDeprecatedIn54(HashAlgorithm.WHIRLPOOL), equalTo(true));
    }
}
