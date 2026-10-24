package com.tavli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SchemaIT {

    @Autowired
    JdbcClient jdbc;

    UUID gameId;

    @BeforeEach
    void insertGame() {
        gameId = UUID.randomUUID();
        jdbc.sql("insert into games (id, status, turn_number, state) values (?, 'IN_PROGRESS', 1, '{}'::jsonb)")
                .params(gameId).update();
        jdbc.sql("insert into moves (game_id, seq, kind, die1, die2) values (?, 1, 'OPENING_THROW', 4, 2)")
                .params(gameId).update();
    }

    @Test
    void flywayAppliedTheInitialMigration() {
        Integer applied = jdbc.sql("select count(*) from flyway_schema_history where version = '1' and success")
                .query(Integer.class).single();

        assertThat(applied).isEqualTo(1);
    }

    @Test
    void movesRejectsUpdatesDeletesAndTruncation() {
        assertThatThrownBy(() -> jdbc.sql("update moves set die1 = 6 where game_id = ?").params(gameId).update())
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.sql("delete from moves where game_id = ?").params(gameId).update())
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.sql("truncate moves").update())
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
    }

    @Test
    void movesRowsMustMatchTheirKind() {
        // A TURN needs a player, a turn number and moves, and no dice.
        assertThatThrownBy(() -> jdbc.sql(
                        "insert into moves (game_id, seq, turn_number, kind, die1, die2) values (?, 2, 1, 'TURN', 3, 1)")
                .params(gameId).update())
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.sql(
                        "insert into moves (game_id, seq, turn_number, kind, player, die1, die2) values (?, 2, 1, 'ROLL', 'WHITE', 7, 1)")
                .params(gameId).update())
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void sequenceNumbersAreUniquePerGame() {
        assertThatThrownBy(() -> jdbc.sql("insert into moves (game_id, seq, kind, die1, die2) values (?, 1, 'OPENING_THROW', 3, 3)")
                .params(gameId).update())
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void finishedGamesNeedAWinner() {
        assertThatThrownBy(() -> jdbc.sql("insert into games (id, status, turn_number, state) values (?, 'FINISHED', 9, '{}'::jsonb)")
                .params(UUID.randomUUID()).update())
                .isInstanceOf(DataAccessException.class);
    }
}
