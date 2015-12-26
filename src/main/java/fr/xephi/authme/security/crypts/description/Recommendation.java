package fr.xephi.authme.security.crypts.description;

/**
 * Annotation to mark a hash algorithm with the usage recommendation, see {@link Usage}.
 */
public @interface Recommendation {

    Usage value();

}
