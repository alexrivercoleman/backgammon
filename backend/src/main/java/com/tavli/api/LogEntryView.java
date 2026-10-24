package com.tavli.api;

import com.tavli.engine.Player;
import com.tavli.game.LogEntry;
import com.tavli.game.MoveJson;
import java.time.Instant;
import java.util.List;

/** A row of the move log; see {@link LogEntry} for which fields each kind uses. */
public record LogEntryView(int seq, LogEntry.Kind kind, Integer turnNumber, Player player, List<Integer> dice,
                           List<MoveJson> moves, Instant createdAt) {

    static LogEntryView of(LogEntry entry) {
        return new LogEntryView(entry.seq(), entry.kind(), entry.turnNumber(), entry.player(), entry.dice(),
                entry.moves() == null ? null : entry.moves().stream().map(MoveJson::of).toList(),
                entry.createdAt());
    }
}
