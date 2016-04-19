package fr.xephi.authme.security.crypts;

import fr.xephi.authme.ConsoleLoggerTestInitializer;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.WrapperMock;
import org.junit.BeforeClass;

/**
 * Test for {@link BCRYPT}.
 */
public class BcryptTest extends AbstractEncryptionMethodTest {

    @BeforeClass
    public static void setUpSettings() {
        WrapperMock.createInstance();
        Settings.bCryptLog2Rounds = 8;
        ConsoleLoggerTestInitializer.setupLogger();
    }

    public BcryptTest() {
        super(new BCRYPT(),
            "$2a$10$6iATmYgwJVc3YONhVcZFve3Cfb5GnwvKhJ20r.hMjmcNkIT9.Uh9K", // password
            "$2a$10$LOhUxhEcS0vgDPv/jkXvCurNb7LjP9xUlEolJGk.Uhgikqc6FtIOi", // PassWord1
            "$2a$10$j9da7SGiaakWhzIms9BtwemLUeIhSEphGUQ3XSlvYgpYsGnGCKRBa", // &^%te$t?Pw@_
            "$2a$10$mkmO3SNzQT/SA5fG/8P8PePz/DI/kKpIH8vd1Owf/fQfFu6F0QyWO"  // âË_3(íù*
        );
    }

}
