package fr.xephi.authme.data.limbo.persistence;

/**
 * Types of persistence for LimboPlayer objects.
 */
public enum LimboPersistenceType {

    INDIVIDUAL_FILES(SeparateFilePersistenceHandler.class),

    SINGLE_FILE(SingleFilePersistenceHandler.class),

    DISABLED(NoOpPersistenceHandler.class);

    private final Class<? extends LimboPersistenceHandler> implementationClass;

    /**
     * Constructor.
     *
     * @param implementationClass the implementation class
     */
    LimboPersistenceType(Class<? extends LimboPersistenceHandler> implementationClass) {
        this.implementationClass=  implementationClass;
    }

    /**
     * @return class implementing the persistence type
     */
    public Class<? extends LimboPersistenceHandler> getImplementationClass() {
        return implementationClass;
    }
}
