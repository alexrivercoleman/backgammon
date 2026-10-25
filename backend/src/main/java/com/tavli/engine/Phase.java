package com.tavli.engine;

/** What the game is waiting for. */
public enum Phase {
    /** Each player throws one die, WHITE first; ties are thrown again. The higher thrower starts. */
    OPENING,
    /** The player to move must roll their dice. */
    ROLLING,
    /** The player to move must play their roll. */
    MOVING,
    /** Someone has borne off all their checkers. */
    FINISHED
}
