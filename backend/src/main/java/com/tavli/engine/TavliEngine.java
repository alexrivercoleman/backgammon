package com.tavli.engine;

import java.util.List;
import java.util.Objects;

/**
 * Entry point for playing Portes: starts games, rolls dice when a player asks, lists legal turns and applies
 * them. Stateless apart from the dice.
 */
public final class TavliEngine {

    private final Dice dice;

    public TavliEngine(Dice dice) {
        this.dice = Objects.requireNonNull(dice, "dice");
    }

    /** Sets up the starting position with WHITE to throw the first opening die. No dice are rolled yet. */
    public GameState start() {
        return GameState.opening();
    }

    /**
     * Rolls for the player whose roll it is.
     * <ul>
     *   <li>OPENING, WHITE to throw: throws WHITE's single die; BLACK throws next.</li>
     *   <li>OPENING, BLACK to throw: throws BLACK's die, completing the round. On a tie the opening starts
     *       again; otherwise the higher thrower moves first, playing both opening dice.</li>
     *   <li>ROLLING: throws two dice for the player to move.</li>
     * </ul>
     *
     * @throws IllegalStateException if the game is waiting for a move or is over
     */
    public RollOutcome roll(GameState state) {
        return switch (state.phase()) {
            case OPENING -> {
                int die = dice.roll();
                if (state.toMove() == Player.WHITE) {
                    yield new RollOutcome(new GameState(Phase.OPENING, state.board(), Player.BLACK, null, die, null),
                            null);
                }
                OpeningThrow round = new OpeningThrow(state.openingDie(), die);
                if (round.isTie()) {
                    yield new RollOutcome(new GameState(Phase.OPENING, state.board(), Player.WHITE, null, null, null),
                            round);
                }
                Player first = round.white() > round.black() ? Player.WHITE : Player.BLACK;
                yield new RollOutcome(GameState.moving(state.board(), first, new Roll(round.white(), round.black())),
                        round);
            }
            case ROLLING -> new RollOutcome(GameState.moving(state.board(), state.toMove(), Roll.of(dice)), null);
            case MOVING, FINISHED -> throw new IllegalStateException("Nothing to roll while " + state.phase());
        };
    }

    /** Legal ways to play the current roll. */
    public TurnPlan plan(GameState state) {
        requireMoving(state);
        return TurnPlan.of(state.board(), state.toMove(), state.roll());
    }

    /**
     * Applies a complete turn (empty when the player must pass). If it bears off the mover's last checker the
     * game is over; otherwise the opponent is to roll.
     *
     * @throws IllegalTurnException  if {@code turn} is not a legal way to play the current roll
     * @throws IllegalStateException if the game is not waiting for a move
     */
    public GameState play(GameState state, List<Move> turn) {
        TurnPlan plan = plan(state);
        if (!plan.isLegal(turn)) {
            throw new IllegalTurnException(state, turn);
        }
        Board board = plan.resultOf(turn);
        Player mover = state.toMove();
        if (board.off(mover) == Board.CHECKERS_PER_PLAYER) {
            return GameState.finished(board, mover);
        }
        return GameState.awaitingRoll(board, mover.opponent());
    }

    private static void requireMoving(GameState state) {
        if (state.phase() != Phase.MOVING) {
            throw new IllegalStateException("Not waiting for a move while " + state.phase());
        }
    }
}
