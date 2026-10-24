package com.tavli.engine;

/** Source of single die rolls. Injected into the engine so tests can supply fixed sequences. */
@FunctionalInterface
public interface Dice {

    /** Returns a value in 1..6. */
    int roll();
}
