package fr.xephi.authme.settings.hierarchicalvalues;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link HierarchicalValues}.
 */
public class HierarchicalValuesTest {

    @Test
    public void shouldCreateContainerWithSingleRootValue() {
        // given
        int value = 1024;
        HierarchicalValues<Integer> container = HierarchicalValues.createContainerWithRoot(value);

        // when / then
        assertThat(container.getValue(""), equalTo(value));
        assertThat(container.getValue(null), equalTo(value));
        assertThat(container.getValue("foo"), equalTo(value));
        assertThat(container.getValue("foo.bar"), equalTo(value));
        assertThat(container.getValue("foo.bar.Baz"), equalTo(value));
    }

    @Test
    public void shouldCreateContainerWithGivenValues() {
        // given
        Map<String, Double> values = ImmutableMap.of(
            "country", 3.0,
            "country.europe", 4.2,
            "country.europe.switzerland", 4.4,
            "country.asia.china", 2.8,
            "country.africa", 2.5);
        HierarchicalValues<Double> container = HierarchicalValues.createContainer(2.9, values);

        // when / then
        assertThat(container.getValue(""), equalTo(2.9));
        assertThat(container.getValue("country"), equalTo(3.0));
        assertThat(container.getValue("country.europe.italy"), equalTo(4.2));
        assertThat(container.getValue("country.europe.switzerland"), equalTo(4.4));
        assertThat(container.getValue("country.europe.switzerland.zug"), equalTo(4.4));
        assertThat(container.getValue("country.asia.japan"), equalTo(3.0));
        assertThat(container.getValue("country.africa"), equalTo(2.5));
        assertThat(container.getValue("country.africa.rwanda"), equalTo(2.5));
        assertThat(container.getValue("other"), equalTo(2.9));
    }

    @Test
    public void shouldCreateContainerWithGivenValuesIncludingRoot() {
        // given
        Map<String, Integer> values = ImmutableMap.of(
            "", 9,
            "foo.bar", 12);
        HierarchicalValues<Integer> container = HierarchicalValues.createContainer(-1337, values);

        // when / then
        assertThat(container.getValue(""), equalTo(9));
        assertThat(container.getValue(null), equalTo(9));
        assertThat(container.getValue("bogus"), equalTo(9));
        assertThat(container.getValue("foo.bar"), equalTo(12));

        assertThat(container.getValue("..."), equalTo(9));
        assertThat(container.getValue(".values.test."), equalTo(9));
        assertThat(container.getValue("foo.bar.child."), equalTo(12));
    }

    @Test
    public void shouldAddAndReplaceValues() {
        // given
        Map<String, Character> values = ImmutableMap.of(
            "1", 'A',
            "2", 'B');
        HierarchicalValues<Character> container = HierarchicalValues.createContainer('0', values);

        // when
        container.addValue("3", 'C');
        container.addValue("1", 'Z');

        // then
        assertThat(container.getValue("1"), equalTo('Z'));
        assertThat(container.getValue("2"), equalTo('B'));
        assertThat(container.getValue("3"), equalTo('C'));
        assertThat(container.getValue("4"), equalTo('0')); // fallback to default
    }

    @Test
    public void shouldReturnAllValuesIncludingRootIfWasSpecificallyAdded() {
        // given
        Map<String, Character> givenValues = ImmutableMap.of(
            "1", 'A',
            "2", 'B');
        HierarchicalValues<Character> container = HierarchicalValues.createContainer('0', givenValues);

        // when / then
        Map<String, Character> values1 = container.createValuesStream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        assertThat(values1, equalTo(givenValues));

        container.addValue("", 'q');
        Map<String, Character> values2 = container.createValuesStream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        assertThat(values2, equalTo(ImmutableMap.of("1", 'A', "2", 'B', "", 'q')));
    }

    @Test
    public void shouldReturnAllValuesIncludingInitiallySpecifiedRoot() {
        // given
        Map<String, Character> givenValues = ImmutableMap.of(
            "1", 'A',
            "", '^');
        HierarchicalValues<Character> container = HierarchicalValues.createContainer('0', givenValues);

        // when
        Map<String, Character> values = container.createValuesStream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // then
        assertThat(values, equalTo(givenValues));
    }
}
