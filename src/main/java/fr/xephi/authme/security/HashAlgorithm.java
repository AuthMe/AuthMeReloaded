package fr.xephi.authme.security;

import fr.xephi.authme.security.crypts.EncryptionMethod;

/**
 * The list of hash algorithms supported by AuthMe. The linked {@link EncryptionMethod} implementation
 * must be able to be instantiated with the default constructor.
 */
public enum HashAlgorithm {

    BCRYPT(fr.xephi.authme.security.crypts.BCRYPT.class),
    BCRYPT2Y(fr.xephi.authme.security.crypts.BCRYPT2Y.class),
    CRAZYCRYPT1(fr.xephi.authme.security.crypts.CRAZYCRYPT1.class),
    DOUBLEMD5(fr.xephi.authme.security.crypts.DOUBLEMD5.class),
    IPB3(fr.xephi.authme.security.crypts.IPB3.class),
    JOOMLA(fr.xephi.authme.security.crypts.JOOMLA.class),
    MD5(fr.xephi.authme.security.crypts.MD5.class),
    MD5VB(fr.xephi.authme.security.crypts.MD5VB.class),
    MYBB(fr.xephi.authme.security.crypts.MYBB.class),
    PBKDF2(fr.xephi.authme.security.crypts.CryptPBKDF2.class),
    PBKDF2DJANGO(fr.xephi.authme.security.crypts.CryptPBKDF2Django.class),
    PHPBB(fr.xephi.authme.security.crypts.PHPBB.class),
    PHPFUSION(fr.xephi.authme.security.crypts.PHPFUSION.class),
    @Deprecated
    PLAINTEXT(fr.xephi.authme.security.crypts.PLAINTEXT.class),
    ROYALAUTH(fr.xephi.authme.security.crypts.ROYALAUTH.class),
    SALTED2MD5(fr.xephi.authme.security.crypts.SALTED2MD5.class),
    SALTEDSHA512(fr.xephi.authme.security.crypts.SALTEDSHA512.class),
    SHA1(fr.xephi.authme.security.crypts.SHA1.class),
    SHA256(fr.xephi.authme.security.crypts.SHA256.class),
    SHA512(fr.xephi.authme.security.crypts.SHA512.class),
    SMF(fr.xephi.authme.security.crypts.SMF.class),
    WBB3(fr.xephi.authme.security.crypts.WBB3.class),
    WBB4(fr.xephi.authme.security.crypts.WBB4.class),
    WHIRLPOOL(fr.xephi.authme.security.crypts.WHIRLPOOL.class),
    WORDPRESS(fr.xephi.authme.security.crypts.WORDPRESS.class),
    XAUTH(fr.xephi.authme.security.crypts.XAUTH.class),
    XFBCRYPT(fr.xephi.authme.security.crypts.XFBCRYPT.class),
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
