package fr.xephi.authme.data.limbo.persistence;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test for {@link SegmentSize}.
 */
class SegmentSizeTest {

    @Test
    void shouldHaveDistributionThatIsPowerOf2() {
        // given
        Set<Integer> allowedDistributions = ImmutableSet.of(1, 2, 4, 8, 16);

        // when / then
        for (SegmentSize entry : SegmentSize.values()) {
            if (!allowedDistributions.contains(entry.getDistribution())) {
                fail("Distribution must be a power of 2 and within [1, 16]. Offending item: " + entry);
            }
        }
    }

    @Test
    void shouldHaveDifferentSegmentSizes() {
        // given
        Set<Integer> segmentTotals = new HashSet<>();

        // when / then
        for (SegmentSize entry : SegmentSize.values()) {
            int totalSegments = entry.getTotalSegments();
            assertThat(entry + " must have a positive segment size",
                totalSegments, greaterThan(0));

            assertThat(entry + " has a segment total that was already encountered (" + totalSegments + ")",
                segmentTotals.add(totalSegments), equalTo(true));
        }
    }
}
