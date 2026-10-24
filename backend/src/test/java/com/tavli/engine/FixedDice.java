package com.tavli.engine;

import java.util.ArrayDeque;
import java.util.Deque;

/** Dice that return a scripted sequence and fail loudly if asked for more. */
final class FixedDice implements Dice {

    private final Deque<Integer> values = new ArrayDeque<>();

    FixedDice(int... values) {
        for (int value : values) {
            this.values.add(value);
        }
    }

    @Override
    public int roll() {
        Integer next = values.poll();
        if (next == null) {
            throw new IllegalStateException("No more scripted rolls");
        }
        return next;
    }

    boolean exhausted() {
        return values.isEmpty();
    }
}
