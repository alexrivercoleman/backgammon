package com.tavli.game.persistence;

import com.tavli.engine.GameState;
import com.tavli.game.Game;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class GameRepository {

    private final JdbcClient jdbc;
    private final StateJson json;

    public GameRepository(JdbcClient jdbc, StateJson json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public void insert(Game game) {
        jdbc.sql("""
                        insert into games (id, status, winner, turn_number, state)
                        values (:id, :status, :winner, :turnNumber, cast(:state as jsonb))
                        """)
                .param("id", game.id())
                .param("status", status(game.state()))
                .param("winner", winner(game.state()))
                .param("turnNumber", game.turnNumber())
                .param("state", json.write(game.state()))
                .update();
    }

    public Optional<Game> find(UUID id) {
        return jdbc.sql("select turn_number, state from games where id = :id")
                .param("id", id)
                .query((rs, row) -> new Game(id, rs.getInt("turn_number"), json.read(rs.getString("state"))))
                .optional();
    }

    /**
     * Replaces the game's state if it is still on {@code expectedTurnNumber}.
     *
     * @return false if the game has moved on (or does not exist), in which case nothing is written
     */
    public boolean update(Game game, int expectedTurnNumber) {
        int rows = jdbc.sql("""
                        update games
                        set status = :status, winner = :winner, turn_number = :turnNumber,
                            state = cast(:state as jsonb), updated_at = now()
                        where id = :id and turn_number = :expected
                        """)
                .param("id", game.id())
                .param("status", status(game.state()))
                .param("winner", winner(game.state()))
                .param("turnNumber", game.turnNumber())
                .param("state", json.write(game.state()))
                .param("expected", expectedTurnNumber)
                .update();
        return rows == 1;
    }

    private static String status(GameState state) {
        return state.isOver() ? "FINISHED" : "IN_PROGRESS";
    }

    private static String winner(GameState state) {
        return state.isOver() ? state.winner().name() : null;
    }
}
