package com.tavli.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Entry point for playing Portes: starts games, lists legal turns and applies them. Stateless apart from dice. */
public final class TavliEngine {

    private final Dice dice;

    public TavliEngine(Dice dice) {
        this.dice = Objects.requireNonNull(dice, "dice");
    }

    /** Sets up the starting position; each player throws one die until they differ, and the higher starts. */
    public Opening start() {
        List<Opening.Throw> throwsMade = new ArrayList<>();
        Opening.Throw opening;
        do {
            opening = new Opening.Throw(dice.roll(), dice.roll());
            throwsMade.add(opening);
        } while (opening.isTie());

        Player first = opening.white() > opening.black() ? Player.WHITE : Player.BLACK;
        Roll roll = new Roll(opening.white(), opening.black());
        return new Opening(throwsMade, new GameState(Board.starting(), first, roll, null));
    }

    /** Legal ways to play the current roll. */
    public TurnPlan plan(GameState state) {
        requireInProgress(state);
        return TurnPlan.of(state.board(), state.toMove(), state.roll());
    }

    /**
     * Applies a complete turn (empty when the player must pass). If it bears off the mover's last checker the
     * game is over; otherwise the opponent is to move with a fresh roll.
     *
     * @throws IllegalTurnException if {@code turn} is not a legal way to play the current roll
     */
    public GameState play(GameState state, List<Move> turn) {
        TurnPlan plan = plan(state);
        if (!plan.isLegal(turn)) {
            throw new IllegalTurnException(state, turn);
        }
        Board board = plan.resultOf(turn);
        Player mover = state.toMove();
        if (board.off(mover) == Board.CHECKERS_PER_PLAYER) {
            return new GameState(board, mover, null, mover);
        }
        return new GameState(board, mover.opponent(), Roll.of(dice), null);
    }

    private static void requireInProgress(GameState state) {
        if (state.isOver()) {
            throw new IllegalStateException("Game is over; " + state.winner() + " won");
        }
    }
}
