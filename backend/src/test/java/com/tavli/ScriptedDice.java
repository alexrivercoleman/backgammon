package com.tavli;

import com.tavli.engine.Dice;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * Dice for integration tests: returns scripted values, then (if enabled) values from a seeded generator, and
 * otherwise fails loudly so an unexpected roll is caught.
 */
public class ScriptedDice implements Dice {

    private final Deque<Integer> script = new ArrayDeque<>();
    private Random fallback;

    public synchronized ScriptedDice script(int... values) {
        for (int value : values) {
            script.add(value);
        }
        return this;
    }

    public synchronized ScriptedDice thenSeeded(long seed) {
        fallback = new Random(seed);
        return this;
    }

    public synchronized void reset() {
        script.clear();
        fallback = null;
    }

    @Override
    public synchronized int roll() {
        Integer next = script.poll();
        if (next != null) {
            return next;
        }
        if (fallback != null) {
            return fallback.nextInt(1, 7);
        }
        throw new IllegalStateException("Unscripted dice roll");
    }
}
