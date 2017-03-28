package fr.xephi.authme.data.limbo.persistence;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static fr.xephi.authme.data.limbo.persistence.SegmentSize.EIGHT;
import static fr.xephi.authme.data.limbo.persistence.SegmentSize.FOUR;
import static fr.xephi.authme.data.limbo.persistence.SegmentSize.ONE;
import static fr.xephi.authme.data.limbo.persistence.SegmentSize.SIXTEEN;
import static fr.xephi.authme.data.limbo.persistence.SegmentSize.SIXTY_FOUR;
import static fr.xephi.authme.data.limbo.persistence.SegmentSize.THIRTY_TWO;
import static fr.xephi.authme.data.limbo.persistence.SegmentSize.TWO_FIFTY;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link SegmentNameBuilder}.
 */
public class SegmentNameBuilderTest {

    /**
     * Checks that using a given segment size really produces as many segments as defined.
     * E.g. if we partition with {@link SegmentSize#EIGHT} we expect eight different buckets.
     */
    @Test
    public void shouldCreatePromisedSizeOfSegments() {
        for (SegmentSize part : SegmentSize.values()) {
            // Perform this check only for `length` <= 5 because the test creates all hex numbers with `length` digits.
            if (part.getLength() <= 5) {
                checkTotalSegmentsProduced(part);
            }
        }
    }

    private void checkTotalSegmentsProduced(SegmentSize part) {
        // given
        SegmentNameBuilder nameBuilder = new SegmentNameBuilder(part);
        Set<String> encounteredSegments = new HashSet<>();
        int shift = part.getLength() * 4;
        // e.g. (1 << 16) - 1 = 0xFFFF. (Number of digits = shift/4, since 16 = 2^4)
        int max = (1 << shift) - 1;

        // when
        for (int i = 0; i <= max; ++i) {
            String uuid = toPaddedHex(i, part.getLength());
            encounteredSegments.add(nameBuilder.createSegmentName(uuid));
        }

        // then
        assertThat(encounteredSegments, hasSize(part.getTotalSegments()));
    }

    private static String toPaddedHex(int dec, int padLength) {
        String hexResult = Integer.toString(dec, 16);
        while (hexResult.length() < padLength) {
            hexResult = "0" + hexResult;
        }
        return hexResult;
    }

    @Test
    public void shouldCreateOneSegment() {
        // given
        SegmentNameBuilder nameBuilder = new SegmentNameBuilder(ONE);

        // when / then
        assertThat(nameBuilder.createSegmentName("abc"), equalTo("seg1-0"));
        assertThat(nameBuilder.createSegmentName("f0e"), equalTo("seg1-0"));
        assertThat(nameBuilder.createSegmentName("329"), equalTo("seg1-0"));
    }

    @Test
    public void shouldCreateFourSegments() {
        // given
        SegmentNameBuilder nameBuilder = new SegmentNameBuilder(FOUR);

        // when / then
        assertThat(nameBuilder.createSegmentName("f9cc"), equalTo("seg4-3"));
        assertThat(nameBuilder.createSegmentName("84c9"), equalTo("seg4-2"));
        assertThat(nameBuilder.createSegmentName("3799"), equalTo("seg4-0"));
    }

    @Test
    public void shouldCreateEightSegments() {
        // given
        SegmentNameBuilder nameBuilder = new SegmentNameBuilder(EIGHT);

        // when / then
        assertThat(nameBuilder.createSegmentName("fc9c"), equalTo("seg8-7"));
        assertThat(nameBuilder.createSegmentName("90ad"), equalTo("seg8-4"));
        assertThat(nameBuilder.createSegmentName("35e4"), equalTo("seg8-1"));
        assertThat(nameBuilder.createSegmentName("a39f"), equalTo("seg8-5"));
    }

    @Test
    public void shouldCreateSixteenSegments() {
        // given
        SegmentNameBuilder nameBuilder = new SegmentNameBuilder(SIXTEEN);

        // when / then
        assertThat(nameBuilder.createSegmentName("fc9a054"), equalTo("seg16-f"));
        assertThat(nameBuilder.createSegmentName("b0a945e"), equalTo("seg16-b"));
        assertThat(nameBuilder.createSegmentName("7afebab"), equalTo("seg16-7"));
    }

    @Test
    public void shouldCreateThirtyTwoSegments() {
        // given
        SegmentNameBuilder nameBuilder = new SegmentNameBuilder(THIRTY_TWO);

        // when / then
        assertThat(nameBuilder.createSegmentName("f890c9"), equalTo("seg32-11101"));
        assertThat(nameBuilder.createSegmentName("49c39a"), equalTo("seg32-01101"));
        assertThat(nameBuilder.createSegmentName("b75d09"), equalTo("seg32-10010"));
    }

    @Test
    public void shouldCreateSixtyFourSegments() {
        // given
        SegmentNameBuilder nameBuilder = new SegmentNameBuilder(SIXTY_FOUR);

        // when / then
        assertThat(nameBuilder.createSegmentName("82f"), equalTo("seg64-203"));
        assertThat(nameBuilder.createSegmentName("9b4"), equalTo("seg64-221"));
        assertThat(nameBuilder.createSegmentName("068"), equalTo("seg64-012"));
    }

    @Test
    public void shouldCreate256Segments() {
        // given
        SegmentNameBuilder nameBuilder = new SegmentNameBuilder(TWO_FIFTY);

        // when / then
        assertThat(nameBuilder.createSegmentName("a813c"), equalTo("seg256-a8"));
        assertThat(nameBuilder.createSegmentName("b4d01"), equalTo("seg256-b4"));
        assertThat(nameBuilder.createSegmentName("7122f"), equalTo("seg256-71"));
    }
}
