package fr.xephi.authme.util;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.empty;

/**
 * Test for {@link CollectionUtils}.
 */
public class CollectionUtilsTest {

    @Test
    public void shouldGetFullList() {
        // given
        List<String> list = Arrays.asList("test", "1", "2", "3", "4");

        // when
        List<String> result = CollectionUtils.getRange(list, 0, 24);

        // then
        assertThat(result, equalTo(list));
    }

    @Test
    public void shouldReturnEmptyListForZeroCount() {
        // given
        List<String> list = Arrays.asList("test", "1", "2", "3", "4");

        // when
        List<String> result = CollectionUtils.getRange(list, 2, 0);

        // then
        assertThat(result, empty());
    }


    @Test
    public void shouldReturnEmptyListForTooHighStart() {
        // given
        List<String> list = Arrays.asList("test", "1", "2", "3", "4");

        // when
        List<String> result = CollectionUtils.getRange(list, 12, 2);

        // then
        assertThat(result, empty());
    }

    @Test
    public void shouldReturnSubList() {
        // given
        List<String> list = Arrays.asList("test", "1", "2", "3", "4");

        // when
        List<String> result = CollectionUtils.getRange(list, 1, 3);

        // then
        assertThat(result, contains("1", "2", "3"));
    }

    @Test
    public void shouldReturnTillEnd() {
        // given
        List<String> list = Arrays.asList("test", "1", "2", "3", "4");

        // when
        List<String> result = CollectionUtils.getRange(list, 2, 3);

        // then
        assertThat(result, contains("2", "3", "4"));
    }

    @Test
    public void shouldRemoveFirstTwo() {
        // given
        List<String> list = Arrays.asList("test", "1", "2", "3", "4");

        // when
        List<String> result = CollectionUtils.getRange(list, 2);

        // then
        assertThat(result, contains("2", "3", "4"));
    }

    @Test
    public void shouldHandleNegativeStart() {
        // given
        List<String> list = Arrays.asList("test", "1", "2", "3", "4");

        // when
        List<String> result = CollectionUtils.getRange(list, -4);

        // then
        assertThat(result, equalTo(list));
    }
}
