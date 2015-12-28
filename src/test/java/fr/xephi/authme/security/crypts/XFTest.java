package fr.xephi.authme.security.crypts;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Test for {@link XF}.
 */
@Ignore
// TODO #369: XF needs to generate a salt it is expecting
public class XFTest {

    @Test
    public void shouldComputeHash() {
        System.out.println(new XF().computeHash("Test", "name"));
    }
}
