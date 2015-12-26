package fr.xephi.authme.security.crypts.description;

/**
 * Describes the type of salt the encryption algorithm uses. This is purely for documentation
 * purposes and is ignored by the code.
 */
public @interface HasSalt {

    SaltType value();

    int length() default 0;

}
