package fr.xephi.authme.listener;

/**
 * Tracks whether a PlayerSpawnLocationEvent has been fired for the current server session.
 * Used to determine whether the spawn teleport fallback in {@link PlayerListener} is needed.
 */
public final class SpawnLocationTracker {

    private static boolean eventCalled = false;

    private SpawnLocationTracker() {
    }

    public static void markEventCalled() {
        eventCalled = true;
    }

    public static boolean isEventCalled() {
        return eventCalled;
    }
}
