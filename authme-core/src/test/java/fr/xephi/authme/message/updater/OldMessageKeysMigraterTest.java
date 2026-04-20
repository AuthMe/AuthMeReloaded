package fr.xephi.authme.message.updater;

import fr.xephi.authme.message.MessageKey;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.fail;

/**
 * Test for {@link OldMessageKeysMigrater}.
 */
public class OldMessageKeysMigraterTest {

    @Test
    public void shouldHasOldKeysThatAreNewParentsFirstInMap() {
        // given
        Set<String> parentPaths = collectParentPathsFromMessageKeys();
        Set<String> encounteredParents = new HashSet<>();

        // when / then
        for (Map.Entry<MessageKey, String> entry : OldMessageKeysMigrater.KEYS_TO_OLD_PATH.entrySet()) {
            if (parentPaths.contains(entry.getValue()) && encounteredParents.contains(entry.getValue())) {
                fail("Entry migrating old path '" + entry.getValue()
                    + "' should come before any new paths with it as parent");
            }
            String parent = entry.getKey().getKey().split("\\.")[0];
            encounteredParents.add(parent);
        }
    }

    private Set<String> collectParentPathsFromMessageKeys() {
        return Arrays.stream(MessageKey.values())
            .map(mk -> mk.getKey().split("\\.")[0])
            .collect(Collectors.toSet());
    }
}
