package com.tavli.game;

import com.tavli.engine.Move;

/**
 * JSON shape of a move, shared by the API and the stored log: {@code from} is a point number or {@code "bar"},
 * {@code to} is a point number or {@code "off"}.
 */
public record MoveJson(Object from, Object to, int die) {

    static final String BAR = "bar";
    static final String OFF = "off";

    public static MoveJson of(Move move) {
        return new MoveJson(
                move.isEntry() ? BAR : (Object) move.from(),
                move.isBearOff() ? OFF : (Object) move.to(),
                move.die());
    }

    /**
     * @throws IllegalArgumentException if a field is not a point number 1..24, the matching keyword, or the die
     *                                  is not 1..6
     */
    public Move toMove() {
        return new Move(location(from, BAR, Move.BAR), location(to, OFF, Move.OFF), die);
    }

    private static int location(Object value, String keyword, int sentinel) {
        if (keyword.equals(value)) {
            return sentinel;
        }
        if (value instanceof Integer point) {
            if (point < 1 || point > 24) {
                throw new IllegalArgumentException("Point must be 1..24, was " + point);
            }
            return point;
        }
        throw new IllegalArgumentException("Expected a point number or \"" + keyword + "\", was " + value);
    }
}
