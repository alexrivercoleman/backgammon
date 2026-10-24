package com.tavli.game;

import com.tavli.engine.Move;
import com.tavli.engine.Player;
import java.time.Instant;
import java.util.List;

/**
 * One row of a game's append-only log.
 *
 * @param turnNumber null for opening throws
 * @param player     null for opening throws
 * @param dice       {@code [white, black]} for an opening throw, the two dice for a roll, null for a turn
 * @param moves      the moves of a turn (empty for a pass), null otherwise
 */
public record LogEntry(int seq, Kind kind, Integer turnNumber, Player player, List<Integer> dice, List<Move> moves,
                       Instant createdAt) {

    public enum Kind {
        OPENING_THROW,
        ROLL,
        TURN
    }
}
