package fr.xephi.authme.security.crypts;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.util.WrapperMock;
import org.junit.BeforeClass;

/**
 * Test for {@link IPB4}.
 */
public class IPB4Test extends AbstractEncryptionMethodTest {

    @BeforeClass
    public static void setUpSettings() {
        WrapperMock.createInstance();
        TestHelper.setupLogger();
    }

    public IPB4Test() {
        super(new IPB4(),
            new HashedPassword("$2a$13$leEvXu77OIwPwNvtZIJvaeAx8EItGHuR3nIlq8416g0gXeJaQdrr2", "leEvXu77OIwPwNvtZIJval"),  //password
            new HashedPassword("$2a$13$xyTTP9zhQQtRRKIJPv5AuuOGJ6Ni9FLbDhcuIAcPjt3XzCxIWe3Uu", "xyTTP9zhQQtRRKIJPv5Au3"),  //PassWord1
            new HashedPassword("$2a$13$rGBrqErm9DZyzbxIGHlgf.xfA15/4d5Ay/TK.3y9lG3AljcoG9Lsi", "rGBrqErm9DZyzbxIGHlgfN"),  //&^%te$t?Pw@_
            new HashedPassword("$2a$13$18dKXZLoGpeHHL81edM9HuipiUcMjn5VDJHlxwRUMRXfJ1b.ZQrJ.", "18dKXZLoGpeHHL81edM9H6")); //âË_3(íù*
    }

}
