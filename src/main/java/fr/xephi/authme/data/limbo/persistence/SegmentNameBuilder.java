package fr.xephi.authme.data.limbo.persistence;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates segment names for {@link DistributedFilesPersistenceHandler}.
 */
class SegmentNameBuilder {

    private final int length;
    private final int distribution;
    private final String prefix;
    private final Map<Character, Character> charToSegmentChar;

    /**
     * Constructor.
     *
     * @param partition the segment configuration
     */
    SegmentNameBuilder(SegmentSize partition) {
        this.length = partition.getLength();
        this.distribution = partition.getDistribution();
        this.prefix = "seg" + partition.getTotalSegments() + "-";
        this.charToSegmentChar = buildCharMap(distribution);
    }

    /**
     * Returns the segment ID for the given UUID.
     *
     * @param uuid the player's uuid to get the segment for
     * @return id the uuid belongs to
     */
    String createSegmentName(String uuid) {
        if (distribution == 16) {
            return prefix + uuid.substring(0, length);
        } else {
            return prefix + buildSegmentName(uuid.substring(0, length).toCharArray());
        }
    }

    /**
     * @return the prefix used for the current segment configuration
     */
    String getPrefix() {
        return prefix;
    }

    private String buildSegmentName(char[] chars) {
        if (chars.length == 1) {
            return String.valueOf(charToSegmentChar.get(chars[0]));
        }

        StringBuilder sb = new StringBuilder(chars.length);
        for (char chr : chars) {
            sb.append(charToSegmentChar.get(chr));
        }
        return sb.toString();
    }

    private static Map<Character, Character> buildCharMap(int distribution) {
        final char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        final int divisor = 16 / distribution;

        Map<Character, Character> charToSegmentChar = new HashMap<>();
        for (int i = 0; i < hexChars.length; ++i) {
            int mappedChar = i / divisor;
            charToSegmentChar.put(hexChars[i], hexChars[mappedChar]);
        }
        return charToSegmentChar;
    }

}
