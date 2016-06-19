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
    private List<PendingInjection> pendingInjections;
    private InjectionResolver injectionResolver;

    public RunDelayedInjects(Statement next, List<PendingInjection> pendingInjections, Object target,
                             InjectionResolver injectionResolver) {
        this.next = next;
        this.pendingInjections = pendingInjections;
        this.target = target;
        this.injectionResolver = injectionResolver;
    }

    @Override
    public void evaluate() throws Throwable {
        for (PendingInjection pendingInjection : pendingInjections) {
            if (ReflectionTestUtils.getFieldValue(pendingInjection.getField(), target) != null) {
                throw new IllegalStateException("Field with @InjectDelayed must be null on startup");
            }
            Object object = injectionResolver.instantiate(pendingInjection.getInjection());
            ReflectionTestUtils.setField(pendingInjection.getField(), target, object);
        }
        this.pendingInjections = null;
        this.injectionResolver = null;
        next.evaluate();
    }
}
