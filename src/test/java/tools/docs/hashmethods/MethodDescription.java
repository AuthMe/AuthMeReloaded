package tools.docs.hashmethods;

import fr.xephi.authme.security.crypts.EncryptionMethod;
import fr.xephi.authme.security.crypts.description.SaltType;
import fr.xephi.authme.security.crypts.description.Usage;

/**
 * Description of a {@link EncryptionMethod}.
 */
public class MethodDescription {

    /** The implementation class the description belongs to. */
    private final Class<? extends EncryptionMethod> method;
    /** The type of the salt that is used. */
    private SaltType saltType;
    /** The length of the salt for SaltType.TEXT salts. */
    private Integer saltLength;
    /** The usage recommendation. */
    private Usage usage;
    /** Whether or not the encryption method is restricted to ASCII characters for proper functioning. */
    private boolean asciiRestricted;
    /** Whether or not the encryption method requires its salt stored separately. */
    private boolean hasSeparateSalt;
    /** The length of the hash output, based on a test hash (i.e. assumes same length for all hashes.) */
    private int hashLength;

    public MethodDescription(Class<? extends EncryptionMethod> method) {
        this.method = method;
    }


    // Trivial getters and setters
    public Class<? extends EncryptionMethod> getMethod() {
        return method;
    }

    public SaltType getSaltType() {
        return saltType;
    }

    public void setSaltType(SaltType saltType) {
        this.saltType = saltType;
    }

    public Integer getSaltLength() {
        return saltLength;
    }

    public void setSaltLength(int saltLength) {
        this.saltLength = saltLength;
    }

    public Usage getUsage() {
        return usage;
    }

    public void setUsage(Usage usage) {
        this.usage = usage;
    }

    public boolean isAsciiRestricted() {
        return asciiRestricted;
    }

    public void setAsciiRestricted(boolean asciiRestricted) {
        this.asciiRestricted = asciiRestricted;
    }

    public boolean hasSeparateSalt() {
        return hasSeparateSalt;
    }

    public void setHasSeparateSalt(boolean hasSeparateSalt) {
        this.hasSeparateSalt = hasSeparateSalt;
    }

    public int getHashLength() {
        return hashLength;
    }

    public void setHashLength(int hashLength) {
        this.hashLength = hashLength;
    }

}
