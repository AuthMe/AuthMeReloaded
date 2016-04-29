package fr.xephi.authme.initialization.samples;

import javax.inject.Inject;

/**
 * Sample - class dependent on alpha and provided.
 */
public class GammaService {

    private AlphaService alphaService;

    @Inject
    public GammaService(AlphaService alphaService) {
        this.alphaService = alphaService;
    }

    public AlphaService getAlphaService() {
        return alphaService;
    }
}
