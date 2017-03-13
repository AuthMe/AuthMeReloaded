package fr.xephi.authme.data.limbo.persistence;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates segment names for {@link SegmentFilesPersistenceHolder}.
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
    SegmentNameBuilder(SegmentConfiguration partition) {
        this.length = partition.getLength();
        this.distribution = partition.getDistribution();
        this.prefix = "seg" + partition.getTotalSegments() + "-";
        this.charToSegmentChar = buildCharMap(distribution);
    }

    String createSegmentName(String uuid) {
        if (distribution == 16) {
            return prefix + uuid.substring(0, length);
        } else {
            return prefix + createSegmentName(uuid.substring(0, length).toCharArray());
        }
    }

    private String createSegmentName(char[] chars) {
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
