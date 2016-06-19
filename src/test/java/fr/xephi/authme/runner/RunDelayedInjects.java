package fr.xephi.authme.runner;

import fr.xephi.authme.ReflectionTestUtils;
import org.junit.runners.model.Statement;

import java.util.List;

/**
 * Statement for initializing {@link InjectDelayed} fields. These fields are
 * constructed after {@link BeforeInjecting} and before JUnit's &#064;Before.
 */
class RunDelayedInjects extends Statement {

    private final Statement next;
    private final Object target;
    private final List<PendingInjection> pendingInjections;

    public RunDelayedInjects(Statement next, List<PendingInjection> pendingInjections, Object target) {
        this.next = next;
        this.pendingInjections = pendingInjections;
        this.target = target;
    }

    @Override
    public void evaluate() throws Throwable {
        for (PendingInjection pendingInjection : pendingInjections) {
            Object object = pendingInjection.instantiate();
            ReflectionTestUtils.setField(pendingInjection.getField(), target, object);
            pendingInjection.clearFields();
        }
        next.evaluate();
    }
}
