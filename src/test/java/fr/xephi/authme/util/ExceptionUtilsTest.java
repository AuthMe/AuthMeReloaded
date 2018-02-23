package fr.xephi.authme.util;

import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.TestHelper;
import org.junit.Test;

import java.util.ConcurrentModificationException;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link ExceptionUtils}.
 */
public class ExceptionUtilsTest {

    @Test
    public void shouldFindWantedThrowable() {
        // given
        ConcurrentModificationException initialCme = new ConcurrentModificationException();
        Throwable th = new Throwable(initialCme);
        ConcurrentModificationException cme = new ConcurrentModificationException(th);
        IllegalStateException ise = new IllegalStateException(cme);
        UnsupportedOperationException uoe = new UnsupportedOperationException(ise);
        ReflectiveOperationException roe = new ReflectiveOperationException(uoe);

        // when
        IllegalStateException resultIse = ExceptionUtils.findThrowableInCause(IllegalStateException.class, roe);
        ConcurrentModificationException resultCme = ExceptionUtils.findThrowableInCause(ConcurrentModificationException.class, cme);
        StackOverflowError resultSoe = ExceptionUtils.findThrowableInCause(StackOverflowError.class, cme);

        // then
        assertThat(resultIse, sameInstance(ise));
        assertThat(resultCme, sameInstance(cme));
        assertThat(resultSoe, nullValue());
    }

    @Test
    public void shouldHandleCircularCausesGracefully() {
        // given
        IllegalStateException ise = new IllegalStateException();
        UnsupportedOperationException uoe = new UnsupportedOperationException(ise);
        ReflectiveOperationException roe = new ReflectiveOperationException(uoe);
        ReflectionTestUtils.setField(Throwable.class, ise, "cause", roe);

        // when
        NullPointerException resultNpe = ExceptionUtils.findThrowableInCause(NullPointerException.class, uoe);
        UnsupportedOperationException resultUoe = ExceptionUtils.findThrowableInCause(UnsupportedOperationException.class, uoe);

        // then
        assertThat(resultNpe, nullValue());
        assertThat(resultUoe, sameInstance(uoe));
    }

    @Test
    public void shouldHaveHiddenConstructor() {
        // given / when / then
        TestHelper.validateHasOnlyPrivateEmptyConstructor(ExceptionUtils.class);
    }
}
