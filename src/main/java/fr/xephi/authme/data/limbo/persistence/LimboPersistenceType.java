package fr.xephi.authme.data.limbo.persistence;

/**
 * Types of persistence for LimboPlayer objects.
 */
public enum LimboPersistenceType {

    /** Store each LimboPlayer in a separate file. */
    INDIVIDUAL_FILES(SeparateFilePersistenceHandler.class),

    /** Store all LimboPlayers in the same file. */
    SINGLE_FILE(SingleFilePersistenceHandler.class),

    /** Distribute LimboPlayers by segments into a set number of files. */
    SEGMENT_FILES(SegmentFilesPersistenceHolder.class),

    /** No persistence to disk. */
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
