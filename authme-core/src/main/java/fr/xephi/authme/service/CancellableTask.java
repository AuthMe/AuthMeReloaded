package fr.xephi.authme.service;

@FunctionalInterface
public interface CancellableTask {

    void cancel();
}
