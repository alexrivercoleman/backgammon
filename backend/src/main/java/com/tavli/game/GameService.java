package com.tavli.game;

import com.tavli.engine.GameState;
import com.tavli.engine.IllegalTurnException;
import com.tavli.engine.Move;
import com.tavli.engine.OpeningThrow;
import com.tavli.engine.Phase;
import com.tavli.engine.Player;
import com.tavli.engine.RollOutcome;
import com.tavli.engine.TavliEngine;
import com.tavli.engine.TurnPlan;
import com.tavli.game.persistence.GameRepository;
import com.tavli.game.persistence.MoveLogRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates and advances games, keeping the stored state and the move log in step. Each action locks the game
 * row, so actions on one game are applied one at a time.
 */
@Service
public class GameService {

    private final TavliEngine engine;
    private final GameRepository games;
    private final MoveLogRepository log;

    public GameService(TavliEngine engine, GameRepository games, MoveLogRepository log) {
        this.engine = engine;
        this.games = games;
        this.log = log;
    }

    /** Starts a game at the opening, with WHITE to throw first. Nothing is rolled or logged yet. */
    @Transactional
    public Game create() {
        Game game = new Game(UUID.randomUUID(), 1, engine.start());
        games.insert(game);
        return game;
    }

    @Transactional(readOnly = true)
    public Game get(UUID id) {
        return games.find(id).orElseThrow(() -> new GameNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<LogEntry> log(UUID id) {
        get(id);
        return log.findByGame(id);
    }

    /** The opening rounds thrown so far, ties included; empty once the first turn has been played. */
    @Transactional(readOnly = true)
    public List<OpeningThrow> openingThrows(Game game) {
        if (game.turnNumber() != 1) {
            return List.of();
        }
        return log.findByGame(game.id()).stream()
                .filter(e -> e.kind() == LogEntry.Kind.OPENING_THROW)
                .map(e -> new OpeningThrow(e.dice().get(0), e.dice().get(1)))
                .toList();
    }

    /** Legal ways to play the current roll, or empty unless the game is waiting for a move. */
    public Optional<TurnPlan> currentTurn(Game game) {
        return game.state().phase() == Phase.MOVING ? Optional.of(engine.plan(game.state())) : Optional.empty();
    }

    /**
     * Rolls for {@code player}: their opening die, or their two dice for the turn. A completed opening round is
     * logged as an opening throw; once a player has dice to play, they are logged as that turn's roll.
     *
     * @throws GameNotFoundException     if there is no such game
     * @throws GameFinishedException     if the game is already over
     * @throws StaleTurnException        if the game is not on {@code turnNumber}
     * @throws ActionNotAllowedException if it is not {@code player}'s roll
     */
    @Transactional
    public Game roll(UUID id, int turnNumber, Player player) {
        Game game = lockedForAction(id, turnNumber);
        GameState state = game.state();
        boolean rolling = state.phase() == Phase.OPENING || state.phase() == Phase.ROLLING;
        if (!rolling || state.toMove() != player) {
            throw new ActionNotAllowedException(rolling
                    ? "It is " + state.toMove() + "'s roll, not " + player + "'s"
                    : "The dice are already rolled; " + state.toMove() + " must move");
        }

        RollOutcome outcome = engine.roll(state);
        Game updated = new Game(id, turnNumber, outcome.state());
        saveFromTurn(updated, turnNumber);
        if (outcome.openingThrow() != null) {
            log.appendOpeningThrow(id, outcome.openingThrow());
        }
        GameState next = outcome.state();
        if (next.phase() == Phase.MOVING) {
            log.appendRoll(id, turnNumber, next.toMove(), next.roll());
        }
        return updated;
    }

    /**
     * Applies a complete turn and logs it; the next player then rolls (unless the turn won the game).
     *
     * @throws GameNotFoundException     if there is no such game
     * @throws GameFinishedException     if the game is already over
     * @throws StaleTurnException        if the game is not on {@code turnNumber}
     * @throws ActionNotAllowedException if the dice have not been rolled yet
     * @throws IllegalTurnException      if {@code moves} is not a legal way to play the current roll
     */
    @Transactional
    public Game play(UUID id, int turnNumber, List<Move> moves) {
        Game game = lockedForAction(id, turnNumber);
        GameState state = game.state();
        if (state.phase() != Phase.MOVING) {
            throw new ActionNotAllowedException(state.toMove() + " must roll before moving");
        }

        GameState next = engine.play(state, moves);
        Game updated = new Game(id, turnNumber + 1, next);
        saveFromTurn(updated, turnNumber);
        log.appendTurn(id, turnNumber, state.toMove(), moves);
        return updated;
    }

    /** Saves the game; the row lock taken on load means it is still on {@code turnNumber}, so this cannot miss. */
    private void saveFromTurn(Game game, int turnNumber) {
        if (!games.update(game, turnNumber)) {
            throw new IllegalStateException("Game " + game.id() + " moved on while locked for turn " + turnNumber);
        }
    }

    /** Loads and locks the game, checking it is still in progress and on the turn the client is playing. */
    private Game lockedForAction(UUID id, int turnNumber) {
        Game game = games.findForUpdate(id).orElseThrow(() -> new GameNotFoundException(id));
        if (game.state().isOver()) {
            throw new GameFinishedException(id, game.state().winner());
        }
        if (game.turnNumber() != turnNumber) {
            throw new StaleTurnException(id, turnNumber, game.turnNumber());
        }
        return game;
    }
}
