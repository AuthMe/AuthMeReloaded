package fr.xephi.authme.process.register;

/**
 * Registration type.
 */
public enum RegistrationType {

    /**
     * Password registration: account is registered with a password supplied by the player.
     */
    PASSWORD,

    /**
     * Email registration: account is registered with an email supplied by the player. A password
     * is generated and sent to the email address.
     */
    EMAIL

}
