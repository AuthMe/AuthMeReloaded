package fr.xephi.authme.runner;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.TestClass;
import org.mockito.InjectMocks;
import org.mockito.Mockito;

/**
 * Validates that {@link DelayedInjectionRunner} is used as intended.
 */
class DelayedInjectionRunnerValidator extends RunListener {

    private final RunNotifier notifier;
    private final TestClass testClass;

    public DelayedInjectionRunnerValidator(RunNotifier notifier, TestClass testClass) {
        this.notifier = notifier;
        this.testClass = testClass;
    }

    @Override
    public void testFinished(Description description) throws Exception {
        try {
            Mockito.validateMockitoUsage();
            if (!testClass.getAnnotatedFields(InjectMocks.class).isEmpty()) {
                throw new IllegalStateException("Do not use @InjectMocks with the DelayedInjectionRunner:" +
                    " use @InjectDelayed or change runner");
            }
        } catch (Throwable t) {
            notifier.fireTestFailure(new Failure(description, t));
        }
    }
}
