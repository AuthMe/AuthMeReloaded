package fr.xephi.authme.security.crypts;

/**
 * Test for {@link XfBCrypt}.
 */
public class XfBCryptTest extends AbstractEncryptionMethodTest {

    public XfBCryptTest() {
        super(new XfBCrypt(),
            "$2a$10$UtuON/ZG.x8EWG/zQbryB.BHfQVrfxk3H7qykzP.UJQ8YiLjZyfqq",  // password
            "$2a$10$Q.ocUo.YtHTdI4nu3pcpKun6BILcmWHm541ANULucmuU/ps1QKY4K",  // PassWord1
            "$2a$10$yHjm02.K4HP5iFU1F..yLeTeo7PWZVbKAr/QGex5jU4.J3mdq/uuO",  // &^%te$t?Pw@_
            "$2a$10$joIayhGStExKWxNbiqMMPOYFSpQ76HVNjpOB7.QwTmG5q.TiJJ.0e"); // âË_3(íù*
    }
}
