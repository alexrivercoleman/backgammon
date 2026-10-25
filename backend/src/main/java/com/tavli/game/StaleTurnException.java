package com.tavli.game;

import java.util.UUID;

/** A turn was submitted for a turn number the game is no longer on, e.g. a double submit. */
public class StaleTurnException extends RuntimeException {

    public StaleTurnException(UUID id, int submitted, int current) {
        super("Game " + id + " is on turn " + current + ", not turn " + submitted);
    }

    /** The game was no longer on {@code expected} when it was saved. */
    public StaleTurnException(UUID id, int expected) {
        super("Game " + id + " is no longer on turn " + expected);
    }
}
