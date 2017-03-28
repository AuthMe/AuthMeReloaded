package fr.xephi.authme.data.limbo.persistence;

/**
 * Configuration for the total number of segments to use.
 * <p>
 * The {@link DistributedFilesPersistenceHandler} reduces the number of files by assigning each UUID
 * to a segment. This enum allows to define how many segments the UUIDs should be distributed in.
 * <p>
 * Segments are defined by a <b>distribution</b> and a <b>length.</b> The distribution defines
 * to how many outputs a single hexadecimal characters should be mapped. So e.g. a distribution
 * of 3 means that all hexadecimal characters 0-f should be distributed over three different
 * outputs evenly. The {@link SegmentNameBuilder} simply uses hexadecimal characters as outputs,
 * so e.g. with a distribution of 3 all hex characters 0-f are mapped to 0, 1, or 2.
 * <p>
 * To ensure an even distribution the segments must be powers of 2. Trivially, to implement a
 * distribution of 16, the same character may be returned as was input (since 0-f make up 16
 * characters). A distribution of 1, on the other hand, means that the same output is returned
 * regardless of the input character.
 * <p>
 * The <b>length</b> parameter defines how many characters of a player's UUID should be used to
 * create the segment ID. In other words, with a distribution of 2 and a length of 3, the first
 * three characters of the UUID are taken into consideration, each mapped to one of two possible
 * characters. For instance, a UUID starting with "0f5c9321" may yield the segment ID "010."
 * Such a segment ID defines in which file the given UUID can be found and stored.
 * <p>
 * The number of segments such a configuration yields is computed as {@code distribution ^ length},
 * since distribution defines how many outputs there are per digit, and length defines the number
 * of digits. For instance, a distribution of 2 and a length of 3 will yield segment IDs 000, 001,
 * 010, 011, 100, 101, 110 and 111 (i.e. all binary numbers from 0 to 7).
 * <p>
 * There are multiple possibilities to achieve certain segment totals, e.g. 8 different segments
 * may be created by setting distribution to 8 and length to 1, or distr. to 2 and length to 3.
 * Where possible, prefer a length of 1 (no string concatenation required) or a distribution of
 * 16 (no remapping of the characters required).
 */
public enum SegmentSize {

    /** 1. */
    ONE(1, 1),

    // /** 2. */
    // TWO(2, 1),

    /** 4. */
    FOUR(4, 1),

    /** 8. */
    EIGHT(8, 1),

    /** 16. */
    SIXTEEN(16, 1),

    /** 32. */
    THIRTY_TWO(2, 5),

    /** 64. */
    SIXTY_FOUR(4, 3),

    /** 128. */
    ONE_TWENTY(2, 7),

    /** 256. */
    TWO_FIFTY(16, 2);

    private final int distribution;
    private final int length;

    SegmentSize(int distribution, int length) {
        this.distribution = distribution;
        this.length = length;
    }

    /**
     * @return the distribution size per character, i.e. how many possible outputs there are
     *         for any hexadecimal character
     */
    public int getDistribution() {
        return distribution;
    }

    /**
     * @return number of characters from a UUID that should be used to create a segment ID
     */
    public int getLength() {
        return length;
    }

    /**
     * @return number of segments to which this configuration will distribute all UUIDs
     */
    public int getTotalSegments() {
        return (int) Math.pow(distribution, length);
    }
}
