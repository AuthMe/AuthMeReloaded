package fr.xephi.authme.util.lazytags;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static fr.xephi.authme.util.lazytags.TagBuilder.createTag;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link TagReplacer}.
 */
public class TagReplacerTest {

    @Test
    public void shouldReplaceTags() {
        // given
        TestTagService tagService = new TestTagService();
        List<Tag<Integer>> tags = tagService.getAvailableTags();
        List<String> messages = Arrays.asList("pi = %PI", "for i = %self, i^2 = %square", "%self %self %PI");

        // when
        TagReplacer<Integer> tagReplacer = TagReplacer.newReplacer(tags, messages);
        List<String> result = tagReplacer.getAdaptedMessages(3);

        // then
        assertThat(tagService.piCount, equalTo(1));
        assertThat(tagService.selfCount, equalTo(1));
        assertThat(tagService.doubleCount, equalTo(0));
        assertThat(tagService.squareCount, equalTo(1));
        assertThat(result, contains("pi = 3.14159", "for i = 3, i^2 = 9", "3 3 3.14159"));
    }

    @Test
    public void shouldNotCallUnusedTags() {
        // given
        TestTagService tagService = new TestTagService();
        List<Tag<Integer>> tags = tagService.getAvailableTags();
        List<String> messages = Arrays.asList("pi = %PI", "double i = %double");

        // when
        TagReplacer<Integer> tagReplacer = TagReplacer.newReplacer(tags, messages);
        List<String> result1 = tagReplacer.getAdaptedMessages(-4);
        List<String> result2 = tagReplacer.getAdaptedMessages(0);

        // then
        assertThat(tagService.piCount, equalTo(2));
        assertThat(tagService.selfCount, equalTo(0));
        assertThat(tagService.doubleCount, equalTo(2));
        assertThat(tagService.squareCount, equalTo(0));
        assertThat(result1, contains("pi = 3.14159", "double i = -8"));
        assertThat(result2, contains("pi = 3.14159", "double i = 0"));
    }

    static final class TestTagService {
        int piCount, selfCount, doubleCount, squareCount;

        String pi() {
            ++piCount;
            return "3.14159";
        }

        String self(int i) {
            ++selfCount;
            return Integer.toString(i);
        }

        String calcDouble(int i) {
            ++doubleCount;
            return Integer.toString(2 * i);
        }

        String calcSquare(int i) {
            ++squareCount;
            return Integer.toString(i * i);
        }

        List<Tag<Integer>> getAvailableTags() {
            return Arrays.asList(
                createTag("%PI", this::pi),
                createTag("%self", this::self),
                createTag("%double", this::calcDouble),
                createTag("%square", this::calcSquare));
        }
    }

}
