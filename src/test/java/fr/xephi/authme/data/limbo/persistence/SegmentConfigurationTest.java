package fr.xephi.authme.data.limbo.persistence;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.fail;

/**
 * Test for {@link SegmentConfiguration}.
 */
public class SegmentConfigurationTest {

    @Test
    public void shouldHaveDistributionThatIsPowerOf2() {
        // given
        Set<Integer> allowedDistributions = ImmutableSet.of(1, 2, 4, 8, 16);

        // when / then
        for (SegmentConfiguration entry : SegmentConfiguration.values()) {
            if (!allowedDistributions.contains(entry.getDistribution())) {
                fail("Distribution must be a power of 2 and within [1, 16]. Offending item: " + entry);
            }
        }
    }

    @Test
    public void shouldHaveDifferentSegmentSizes() {
        // given
        Set<Integer> sizes = new HashSet<>();

        // when / then
        for (SegmentConfiguration entry : SegmentConfiguration.values()) {
            int segSize = (int) Math.pow(entry.getDistribution(), entry.getLength());
            assertThat(entry + " must have a positive segment size",
                segSize, greaterThan(0));

            assertThat(entry + " has a segment size that was already encountered (" + segSize + ")",
                sizes.add(segSize), equalTo(true));
        }
    }
}
