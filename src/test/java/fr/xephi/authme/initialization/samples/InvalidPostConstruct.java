package fr.xephi.authme.initialization.samples;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 * Class with invalid @PostConstruct method.
 */
public class InvalidPostConstruct {

    @Inject
    private AlphaService alphaService;
    @Inject
    private ProvidedClass providedClass;

    @PostConstruct
    public void invalidPostConstr(BetaManager betaManager) {
    }
}
