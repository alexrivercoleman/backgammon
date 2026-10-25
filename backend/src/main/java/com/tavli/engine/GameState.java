package com.tavli.engine;

import java.util.Objects;

/**
 * A game between actions.
 *
 * @param toMove     OPENING: who throws their opening die next (WHITE first); ROLLING and MOVING: whose turn
 *                   it is; FINISHED: the winner
 * @param roll       the dice {@code toMove} must play; only while MOVING
 * @param openingDie WHITE's opening die once thrown, while BLACK has yet to throw; otherwise null
 * @param winner     the player who bore off all their checkers; only when FINISHED
 */
public record GameState(Phase phase, Board board, Player toMove, Roll roll, Integer openingDie, Player winner) {

    public GameState {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(toMove, "toMove");
        if ((phase == Phase.MOVING) != (roll != null)) {
            throw new IllegalArgumentException("A roll is present exactly while MOVING");
        }
        if ((phase == Phase.FINISHED) != (winner != null)) {
            throw new IllegalArgumentException("A winner is present exactly when FINISHED");
        }
        if (winner != null && (winner != toMove || board.off(winner) != Board.CHECKERS_PER_PLAYER)) {
            throw new IllegalArgumentException("The winner must be toMove and have borne off every checker");
        }
        boolean blackToThrow = phase == Phase.OPENING && toMove == Player.BLACK;
        if (blackToThrow != (openingDie != null)) {
            throw new IllegalArgumentException("WHITE's opening die is held exactly while BLACK has to throw");
        }
        if (openingDie != null) {
            Roll.requireDie(openingDie);
        }
    }

    /** A new game: the starting position, with WHITE to throw the first opening die. */
    public static GameState opening() {
        return new GameState(Phase.OPENING, Board.starting(), Player.WHITE, null, null, null);
    }

    public static GameState awaitingRoll(Board board, Player toMove) {
        return new GameState(Phase.ROLLING, board, toMove, null, null, null);
    }

    public static GameState moving(Board board, Player toMove, Roll roll) {
        return new GameState(Phase.MOVING, board, toMove, roll, null, null);
    }

    public static GameState finished(Board board, Player winner) {
        return new GameState(Phase.FINISHED, board, winner, null, null, winner);
    }

    public boolean isOver() {
        return phase == Phase.FINISHED;
    }
}
