package com.tavli.game;

import com.tavli.engine.GameState;
import com.tavli.engine.IllegalTurnException;
import com.tavli.engine.Move;
import com.tavli.engine.Opening;
import com.tavli.engine.TavliEngine;
import com.tavli.engine.TurnPlan;
import com.tavli.game.persistence.GameRepository;
import com.tavli.game.persistence.MoveLogRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates and advances games, keeping the stored state and the move log in step. */
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

    /** Starts a game: logs every opening throw, then the starting player's roll for turn 1. */
    @Transactional
    public Game create() {
        Opening opening = engine.start();
        GameState state = opening.state();
        Game game = new Game(UUID.randomUUID(), 1, state);
        games.insert(game);
        opening.throwsMade().forEach(t -> log.appendOpeningThrow(game.id(), t));
        log.appendRoll(game.id(), game.turnNumber(), state.toMove(), state.roll());
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

    /** Legal ways to play the current turn, or empty once the game is over. */
    public Optional<TurnPlan> currentTurn(Game game) {
        return game.state().isOver() ? Optional.empty() : Optional.of(engine.plan(game.state()));
    }

    /**
     * Applies a complete turn and logs it, followed by the next player's roll unless the turn won the game.
     *
     * @throws GameNotFoundException  if there is no such game
     * @throws GameFinishedException  if the game is already over
     * @throws StaleTurnException     if the game is not on {@code turnNumber}
     * @throws IllegalTurnException   if {@code moves} is not a legal way to play the current roll
     */
    @Transactional
    public Game play(UUID id, int turnNumber, List<Move> moves) {
        Game game = get(id);
        GameState state = game.state();
        if (state.isOver()) {
            throw new GameFinishedException(id, state.winner());
        }
        if (game.turnNumber() != turnNumber) {
            throw new StaleTurnException(id, turnNumber, game.turnNumber());
        }

        GameState next = engine.play(state, moves);
        Game updated = new Game(id, turnNumber + 1, next);
        // Guards against a concurrent submit of the same turn; the update also locks the row for the log appends.
        if (!games.update(updated, turnNumber)) {
            throw new StaleTurnException(id, turnNumber, games.find(id).map(Game::turnNumber).orElse(-1));
        }
        log.appendTurn(id, turnNumber, state.toMove(), moves);
        if (!next.isOver()) {
            log.appendRoll(id, updated.turnNumber(), next.toMove(), next.roll());
        }
        return updated;
    }
}
