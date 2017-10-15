package fr.xephi.authme.command.executable.authme.debug;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Consistency test for {@link MySqlDefaultChanger.Columns} enum.
 */
public class MySqlDefaultChangerColumnsTest {

    @Test
    public void shouldAllHaveDifferentNameProperty() {
        // given
        Set<String> properties = new HashSet<>();

        // when / then
        for (MySqlDefaultChanger.Columns col : MySqlDefaultChanger.Columns.values()) {
            if (!properties.add(col.columnName.getPath())) {
                fail("Column '" + col + "' has a column name property path that was already encountered: "
                    + col.columnName.getPath());
            }
        }
    }

    @Test
    public void shouldHaveMatchingNullableAndNotNullDefinition() {
        for (MySqlDefaultChanger.Columns col : MySqlDefaultChanger.Columns.values()) {
            verifyHasCorrespondingColumnDefinitions(col);
        }
    }

    @Test
    public void shouldHaveMatchingDefaultValueInNotNullDefinition() {
        for (MySqlDefaultChanger.Columns col : MySqlDefaultChanger.Columns.values()) {
            verifyHasSameDefaultValueInNotNullDefinition(col);
        }
    }

    private void verifyHasCorrespondingColumnDefinitions(MySqlDefaultChanger.Columns column) {
        // given / when
        String nullable = column.nullableDefinition;
        String notNull = column.notNullDefinition;

        // then
        String expectedNotNull = nullable + " NOT NULL DEFAULT ";
        assertThat(column.name(), notNull.startsWith(expectedNotNull), equalTo(true));
        // Check that `notNull` length is bigger because we expect a value after DEFAULT
        assertThat(column.name(), notNull.length() > expectedNotNull.length(), equalTo(true));
    }

    private void verifyHasSameDefaultValueInNotNullDefinition(MySqlDefaultChanger.Columns column) {
        // given / when
        String notNull = column.notNullDefinition;
        Object defaultValue = column.defaultValue;

        // then
        String defaultValueAsString = String.valueOf(defaultValue);
        if (!notNull.endsWith("DEFAULT " + defaultValueAsString)
                && !notNull.endsWith("DEFAULT '" + defaultValueAsString + "'")) {
            fail("Expected '" + column + "' not-null definition to contain DEFAULT " + defaultValueAsString);
        }
    }
}
