package com.tavli.game.persistence;

import com.tavli.engine.Board;
import com.tavli.engine.GameState;
import com.tavli.engine.Move;
import com.tavli.engine.Phase;
import com.tavli.engine.Player;
import com.tavli.engine.Roll;
import com.tavli.game.MoveJson;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Converts engine state and moves to and from the JSON stored in Postgres. The stored format is defined here,
 * versioned, and independent of the engine's records.
 *
 * <p>Version 2 adds {@code phase} and {@code openingDie}. Version 1 rows (from before players rolled their own
 * dice) are still read: a game in progress then always had its roll, so it was waiting for a move.
 */
@Component
public class StateJson {

    static final int VERSION = 2;

    private record StoredBoard(List<Integer> points, Map<Player, Integer> bar, Map<Player, Integer> off) {
    }

    private record StoredState(int v, Phase phase, StoredBoard board, Player toMove, List<Integer> roll,
                               Integer openingDie, Player winner) {
    }

    private static final TypeReference<List<MoveJson>> MOVE_LIST = new TypeReference<>() {
    };

    private final JsonMapper mapper;

    public StateJson(JsonMapper mapper) {
        this.mapper = mapper;
    }

    public String write(GameState state) {
        Board board = state.board();
        StoredBoard storedBoard = new StoredBoard(board.points(),
                Map.of(Player.WHITE, board.whiteBar(), Player.BLACK, board.blackBar()),
                Map.of(Player.WHITE, board.whiteOff(), Player.BLACK, board.blackOff()));
        List<Integer> roll = state.roll() == null ? null : List.of(state.roll().first(), state.roll().second());
        return mapper.writeValueAsString(new StoredState(VERSION, state.phase(), storedBoard, state.toMove(), roll,
                state.openingDie(), state.winner()));
    }

    /** @throws IllegalStateException if the stored JSON is an unknown version or describes an invalid state */
    public GameState read(String json) {
        StoredState stored = mapper.readValue(json, StoredState.class);
        if (stored.v() != VERSION && stored.v() != 1) {
            throw new IllegalStateException("Unsupported stored state version " + stored.v());
        }
        try {
            StoredBoard b = stored.board();
            Board board = new Board(b.points(), b.bar().get(Player.WHITE), b.bar().get(Player.BLACK),
                    b.off().get(Player.WHITE), b.off().get(Player.BLACK));
            Roll roll = stored.roll() == null ? null : new Roll(stored.roll().get(0), stored.roll().get(1));
            Phase phase = stored.v() == 1
                    ? (stored.winner() == null ? Phase.MOVING : Phase.FINISHED)
                    : stored.phase();
            return new GameState(phase, board, stored.toMove(), roll, stored.openingDie(), stored.winner());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalStateException("Stored state is invalid: " + json, e);
        }
    }

    public String writeMoves(List<Move> moves) {
        return mapper.writeValueAsString(moves.stream().map(MoveJson::of).toList());
    }

    public List<Move> readMoves(String json) {
        return mapper.readValue(json, MOVE_LIST).stream().map(MoveJson::toMove).toList();
    }
}
