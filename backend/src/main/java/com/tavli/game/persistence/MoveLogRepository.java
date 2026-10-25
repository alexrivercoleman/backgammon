package com.tavli.game.persistence;

import com.tavli.engine.Move;
import com.tavli.engine.OpeningThrow;
import com.tavli.engine.Player;
import com.tavli.engine.Roll;
import com.tavli.game.LogEntry;
import com.tavli.game.LogEntry.Kind;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Appends to and reads the {@code moves} log. Each entry takes the game's next sequence number; callers hold the
 * game row's lock (by updating it in the same transaction) so concurrent appends cannot race.
 */
@Repository
public class MoveLogRepository {

    private final JdbcClient jdbc;
    private final StateJson json;

    public MoveLogRepository(JdbcClient jdbc, StateJson json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public void appendOpeningThrow(UUID gameId, OpeningThrow opening) {
        append(gameId, Kind.OPENING_THROW, null, null, opening.white(), opening.black(), null);
    }

    public void appendRoll(UUID gameId, int turnNumber, Player player, Roll roll) {
        append(gameId, Kind.ROLL, turnNumber, player, roll.first(), roll.second(), null);
    }

    public void appendTurn(UUID gameId, int turnNumber, Player player, List<Move> moves) {
        append(gameId, Kind.TURN, turnNumber, player, null, null, json.writeMoves(moves));
    }

    public List<LogEntry> findByGame(UUID gameId) {
        return jdbc.sql("""
                        select seq, kind, turn_number, player, die1, die2, moves, created_at
                        from moves where game_id = :gameId order by seq
                        """)
                .param("gameId", gameId)
                .query(this::toEntry)
                .list();
    }

    private void append(UUID gameId, Kind kind, Integer turnNumber, Player player, Integer die1, Integer die2,
                        String moves) {
        // Explicit casts give Postgres the parameter types it cannot infer inside INSERT ... SELECT.
        jdbc.sql("""
                        insert into moves (game_id, seq, turn_number, kind, player, die1, die2, moves)
                        select :gameId, coalesce(max(seq), 0) + 1, cast(:turnNumber as integer), cast(:kind as text),
                               cast(:player as text), cast(:die1 as smallint), cast(:die2 as smallint),
                               cast(:moves as jsonb)
                        from moves where game_id = :gameId
                        """)
                .param("gameId", gameId)
                .param("turnNumber", turnNumber)
                .param("kind", kind.name())
                .param("player", player == null ? null : player.name())
                .param("die1", die1)
                .param("die2", die2)
                .param("moves", moves)
                .update();
    }

    private LogEntry toEntry(ResultSet rs, int row) throws SQLException {
        Integer turnNumber = rs.getObject("turn_number", Integer.class);
        String player = rs.getString("player");
        Integer die1 = rs.getObject("die1", Integer.class);
        String moves = rs.getString("moves");
        return new LogEntry(
                rs.getInt("seq"),
                Kind.valueOf(rs.getString("kind")),
                turnNumber,
                player == null ? null : Player.valueOf(player),
                die1 == null ? null : List.of(die1, rs.getInt("die2")),
                moves == null ? null : json.readMoves(moves),
                rs.getObject("created_at", OffsetDateTime.class).toInstant());
    }
}
