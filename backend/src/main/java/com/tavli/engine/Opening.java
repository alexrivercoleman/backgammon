package com.tavli.engine;

import java.util.List;

/**
 * Result of starting a game: each opening throw (one die per player, ties re-rolled) and the resulting state,
 * in which the higher roller moves first with both opening dice.
 */
public record Opening(List<Throw> throwsMade, GameState state) {

    public record Throw(int white, int black) {

        public Throw {
            Roll.requireDie(white);
            Roll.requireDie(black);
        }

        public boolean isTie() {
            return white == black;
        }
    }

    public Opening {
        throwsMade = List.copyOf(throwsMade);
    }
}
