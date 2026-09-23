package fr.xephi.authme.util.lazytags;

import org.junit.jupiter.api.Test;

import java.util.function.Function;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

/**
 * Test for {@link TagBuilder}.
 */
class TagBuilderTest {

    @Test
    void shouldCreateNoArgsTag() {
        // given
        Supplier<String> supplier = () -> "hello";

        // when
        Tag<String> tag = TagBuilder.createTag("hey", supplier);

        // then
        assertThat(tag, instanceOf(SimpleTag.class));
        assertThat(tag.getValue(null), equalTo("hello"));
    }

    @Test
    void shouldCreateDependentTag() {
        // given
        Function<Double, String> function = d -> Double.toString(d + d/10);

        // when
        Tag<Double> tag = TagBuilder.createTag("%test", function);

        // then
        assertThat(tag, instanceOf(DependentTag.class));
        assertThat(tag.getValue(24d), equalTo("26.4"));
    }
}
