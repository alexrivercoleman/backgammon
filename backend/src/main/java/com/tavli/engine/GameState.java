package com.tavli.engine;

import java.util.Objects;

/**
 * A game between turns.
 *
 * @param toMove the player whose turn it is; once the game is over, the winner
 * @param roll   the dice {@code toMove} must play, or {@code null} once the game is over
 * @param winner the player who bore off all their checkers, or {@code null} while the game is in progress
 */
public record GameState(Board board, Player toMove, Roll roll, Player winner) {

    public GameState {
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(toMove, "toMove");
        if (winner == null && roll == null) {
            throw new IllegalArgumentException("A game in progress needs a roll");
        }
        if (winner != null && (roll != null || winner != toMove)) {
            throw new IllegalArgumentException("A finished game has no roll and the winner as toMove");
        }
        if (winner != null && board.off(winner) != Board.CHECKERS_PER_PLAYER) {
            throw new IllegalArgumentException("The winner must have borne off every checker");
        }
    }

    public boolean isOver() {
        return winner != null;
    }
}
