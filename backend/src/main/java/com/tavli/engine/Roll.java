package com.tavli.engine;

import java.util.List;

/** The two dice thrown for a turn. */
public record Roll(int first, int second) {

    public Roll {
        requireDie(first);
        requireDie(second);
    }

    public static Roll of(Dice dice) {
        return new Roll(dice.roll(), dice.roll());
    }

    public boolean isDouble() {
        return first == second;
    }

    public int higher() {
        return Math.max(first, second);
    }

    public int lower() {
        return Math.min(first, second);
    }

    /** The dice available to move with: four of the same value for doubles, otherwise both values. */
    public List<Integer> dice() {
        return isDouble() ? List.of(first, first, first, first) : List.of(first, second);
    }

    static void requireDie(int value) {
        if (value < 1 || value > 6) {
            throw new IllegalArgumentException("Die value must be 1..6, was " + value);
        }
    }
}
