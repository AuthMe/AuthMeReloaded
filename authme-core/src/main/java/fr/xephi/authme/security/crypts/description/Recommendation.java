package fr.xephi.authme.security.crypts.description;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a hash algorithm with the usage recommendation.
 *
 * @see Usage
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Recommendation {

    /**
     * The recommendation for using the hash algorithm.
     *
     * @return The recommended usage
     */
    Usage value();

}
