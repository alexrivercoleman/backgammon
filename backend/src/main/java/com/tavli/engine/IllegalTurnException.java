package com.tavli.engine;

import java.util.List;

/** A submitted turn is not one of the legal ways to play the current roll. */
public class IllegalTurnException extends RuntimeException {

    private final List<Move> turn;

    public IllegalTurnException(GameState state, List<Move> turn) {
        super("Illegal turn for " + state.toMove() + " with " + state.roll() + ": " + turn);
        this.turn = List.copyOf(turn);
    }

    public List<Move> turn() {
        return turn;
    }
}
