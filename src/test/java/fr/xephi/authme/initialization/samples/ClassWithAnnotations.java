package fr.xephi.authme.initialization.samples;

import javax.inject.Inject;

public class ClassWithAnnotations {

    private int size;
    private GammaService gammaService;
    private long duration;

    @Inject
    ClassWithAnnotations(@Size int size, GammaService gammaService, @Duration long duration) {
        this.size = size;
        this.gammaService = gammaService;
        this.duration = duration;
    }

    public int getSize() {
        return size;
    }

    public GammaService getGammaService() {
        return gammaService;
    }

    public long getDuration() {
        return duration;
    }
}
