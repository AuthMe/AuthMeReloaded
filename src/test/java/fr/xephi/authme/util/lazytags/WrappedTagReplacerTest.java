package fr.xephi.authme.util.lazytags;

import fr.xephi.authme.util.lazytags.TagReplacerTest.TestTagService;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link WrappedTagReplacer}.
 */
public class WrappedTagReplacerTest {

    @Test
    public void shouldApplyTags() {
        // given
        TestTagService tagService = new TestTagService();
        List<Tag<Integer>> tags = tagService.getAvailableTags();
        List<SampleClass> objects = Arrays.asList(
            new SampleClass(3, "pi is %PI"),
            new SampleClass(5, "no tags here"),
            new SampleClass(7, "i+i = %double"));

        // when
        WrappedTagReplacer<SampleClass, Integer> replacer = new WrappedTagReplacer<>(
            tags, objects, SampleClass::getDescription, (o, s) -> new SampleClass(o.number, s));
        List<SampleClass> result1 = replacer.getAdaptedItems(8);
        List<SampleClass> result2 = replacer.getAdaptedItems(1);

        // then
        assertThat(tagService.piCount, equalTo(2));
        assertThat(tagService.selfCount, equalTo(0));
        assertThat(tagService.doubleCount, equalTo(2));
        assertThat(tagService.squareCount, equalTo(0));
        assertThat(result1, contains(
            sampleClass(3, "pi is 3.14159"), sampleClass(5, "no tags here"), sampleClass(7, "i+i = 16")));
        assertThat(result2, contains(
            sampleClass(3, "pi is 3.14159"), sampleClass(5, "no tags here"), sampleClass(7, "i+i = 2")));
    }


    private static Matcher<SampleClass> sampleClass(int number, String description) {
        return new TypeSafeMatcher<SampleClass>() {
            @Override
            protected boolean matchesSafely(SampleClass item) {
                return number == item.number && description.equals(item.description);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("SampleClass[number=" + number + ";description=" + description + "]");
            }
        };
    }

    private static final class SampleClass {
        private final int number;
        private final String description;

        SampleClass(int number, String description) {
            this.number = number;
            this.description = description;
        }

        String getDescription() {
            return description;
        }
    }
}
