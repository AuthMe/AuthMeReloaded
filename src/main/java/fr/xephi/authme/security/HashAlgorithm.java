package fr.xephi.authme.security;

import fr.xephi.authme.security.crypts.EncryptionMethod;

/**
 * Hash algorithms supported by AuthMe.
 */
public enum HashAlgorithm {

    BCRYPT(fr.xephi.authme.security.crypts.BCrypt.class),
    BCRYPT2Y(fr.xephi.authme.security.crypts.BCrypt2y.class),
    CRAZYCRYPT1(fr.xephi.authme.security.crypts.CrazyCrypt1.class),
    DOUBLEMD5(fr.xephi.authme.security.crypts.DoubleMD5.class),
    IPB3(fr.xephi.authme.security.crypts.IPB3.class),
    IPB4(fr.xephi.authme.security.crypts.IPB4.class),
    JOOMLA(fr.xephi.authme.security.crypts.Joomla.class),
    MD5(fr.xephi.authme.security.crypts.MD5.class),
    MD5VB(fr.xephi.authme.security.crypts.MD5vB.class),
    MYBB(fr.xephi.authme.security.crypts.MyBB.class),
    PBKDF2(fr.xephi.authme.security.crypts.Pbkdf2.class),
    PBKDF2DJANGO(fr.xephi.authme.security.crypts.Pbkdf2Django.class),
    PHPBB(fr.xephi.authme.security.crypts.PHPBB.class),
    PHPFUSION(fr.xephi.authme.security.crypts.PHPFusion.class),
    @Deprecated
    PLAINTEXT(fr.xephi.authme.security.crypts.PlainText.class),
    ROYALAUTH(fr.xephi.authme.security.crypts.RoyalAuth.class),
    SALTED2MD5(fr.xephi.authme.security.crypts.Salted2MD5.class),
    SALTEDSHA512(fr.xephi.authme.security.crypts.SaltedSHA512.class),
    SHA1(fr.xephi.authme.security.crypts.SHA1.class),
    SHA256(fr.xephi.authme.security.crypts.SHA256.class),
    SHA512(fr.xephi.authme.security.crypts.SHA512.class),
    SMF(fr.xephi.authme.security.crypts.SMF.class),
    TWO_FACTOR(fr.xephi.authme.security.crypts.TwoFactor.class),
    WBB3(fr.xephi.authme.security.crypts.WBB3.class),
    WBB4(fr.xephi.authme.security.crypts.WBB4.class),
    WHIRLPOOL(fr.xephi.authme.security.crypts.Whirlpool.class),
    WORDPRESS(fr.xephi.authme.security.crypts.Wordpress.class),
    XAUTH(fr.xephi.authme.security.crypts.XAuth.class),
    XFBCRYPT(fr.xephi.authme.security.crypts.XFBCrypt.class),
    CUSTOM(null);

    private final Class<? extends EncryptionMethod> clazz;

    /**
     * Constructor for HashAlgorithm.
     *
     * @param clazz The class of the hash implementation.
     */
    HashAlgorithm(Class<? extends EncryptionMethod> clazz) {
        this.clazz = clazz;
    }

    public Class<? extends EncryptionMethod> getClazz() {
        return clazz;
    }

}
