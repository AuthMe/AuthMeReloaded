package fr.xephi.authme.util;

import org.junit.Test;

import java.net.MalformedURLException;
import java.util.ConcurrentModificationException;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

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
        ExceptionWithSettableCause exceptionWithSettableCause = new ExceptionWithSettableCause();
        UnsupportedOperationException uoe = new UnsupportedOperationException(exceptionWithSettableCause);
        exceptionWithSettableCause.cause = new ReflectiveOperationException(uoe);

        // when
        NullPointerException resultNpe = ExceptionUtils.findThrowableInCause(NullPointerException.class, uoe);
        UnsupportedOperationException resultUoe = ExceptionUtils.findThrowableInCause(UnsupportedOperationException.class, uoe);

        // then
        assertThat(resultNpe, nullValue());
        assertThat(resultUoe, sameInstance(uoe));
    }

    @Test
    public void shouldFormatException() {
        // given
        MalformedURLException ex = new MalformedURLException("Unrecognized URL format");

        // when
        String result = ExceptionUtils.formatException(ex);

        // then
        assertThat(result, equalTo("[MalformedURLException]: Unrecognized URL format"));
    }

    private static final class ExceptionWithSettableCause extends Exception {

        Exception cause;

        @Override
        public synchronized Throwable getCause() {
            return cause;
        }
    }
}
