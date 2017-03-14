package fr.xephi.authme.data.limbo.persistence;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link LimboPersistenceType}.
 */
public class LimboPersistenceTypeTest {

    @Test
    public void shouldHaveUniqueImplementationClasses() {
        // given
        Set<Class<?>> classes = new HashSet<>();

        // when / then
        for (LimboPersistenceType persistenceType : LimboPersistenceType.values()) {
            if (!classes.add(persistenceType.getImplementationClass())) {
                fail("Implementation class '" + persistenceType.getImplementationClass() + "' from '"
                    + persistenceType + "' already encountered previously");
            }
        }
    }

    @Test
    public void shouldHaveTypeReturnedFromImplementationClass() {
        for (LimboPersistenceType persistenceType : LimboPersistenceType.values()) {
            // given
            LimboPersistenceHandler implementationMock = mock(persistenceType.getImplementationClass());
            given(implementationMock.getType()).willCallRealMethod();

            // when
            LimboPersistenceType returnedType = implementationMock.getType();

            // then
            assertThat(returnedType, equalTo(persistenceType));
        }
    }

}
