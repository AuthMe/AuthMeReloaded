package fr.xephi.authme.initialization.samples;

import fr.xephi.authme.initialization.Reloadable;

import javax.inject.Inject;

/**
 * Sample - class dependent on alpha service.
 */
public class GammaService implements Reloadable {

    private AlphaService alphaService;
    private boolean wasReloaded;

    @Inject
    public GammaService(AlphaService alphaService) {
        this.alphaService = alphaService;
    }

    public AlphaService getAlphaService() {
        return alphaService;
    }

    @Override
    public void reload() {
        wasReloaded = true;
    }

    public boolean getWasReloaded() {
        return wasReloaded;
    }
}
