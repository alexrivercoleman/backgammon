package com.tavli.game.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tavli.IntegrationTestConfiguration;
import com.tavli.engine.Board;
import com.tavli.engine.GameState;
import com.tavli.engine.Move;
import com.tavli.engine.OpeningThrow;
import com.tavli.engine.Player;
import com.tavli.engine.Roll;
import com.tavli.game.Game;
import com.tavli.game.LogEntry;
import com.tavli.game.StaleTurnException;
import com.tavli.game.LogEntry.Kind;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@SpringBootTest
@Import(IntegrationTestConfiguration.class)
class PersistenceIT {

    @Autowired
    GameRepository games;

    @Autowired
    MoveLogRepository log;

    private static Game newGame() {
        return new Game(UUID.randomUUID(), 1, GameState.moving(Board.starting(), Player.WHITE, new Roll(6, 1)));
    }

    @Test
    void insertedGamesCanBeFound() {
        Game game = newGame();

        games.insert(game);

        assertThat(games.find(game.id())).contains(game);
        assertThat(games.find(UUID.randomUUID())).isEmpty();
    }

    @Test
    void updateAppliesOnlyFromTheExpectedTurn() {
        Game game = newGame();
        games.insert(game);
        Game next = new Game(game.id(), 2, GameState.moving(Board.starting(), Player.BLACK, new Roll(2, 2)));

        games.update(next, 1);

        assertThatThrownBy(() -> games.update(next, 1)).isInstanceOf(StaleTurnException.class);
        assertThatThrownBy(() -> games.update(new Game(UUID.randomUUID(), 2, next.state()), 1))
                .isInstanceOf(StaleTurnException.class);
        assertThat(games.find(game.id())).contains(next);
    }

    @Test
    void logEntriesAreNumberedInOrderAndReadBack() {
        Game game = newGame();
        games.insert(game);
        List<Move> moves = List.of(new Move(Move.BAR, 20, 5), new Move(3, Move.OFF, 4));

        log.appendOpeningThrow(game.id(), new OpeningThrow(4, 4));
        log.appendOpeningThrow(game.id(), new OpeningThrow(6, 1));
        log.appendRoll(game.id(), 1, Player.WHITE, new Roll(6, 1));
        log.appendTurn(game.id(), 1, Player.WHITE, moves);
        log.appendTurn(game.id(), 2, Player.BLACK, List.of());

        List<LogEntry> entries = log.findByGame(game.id());

        assertThat(entries).extracting(LogEntry::seq).containsExactly(1, 2, 3, 4, 5);
        assertThat(entries).extracting(LogEntry::kind)
                .containsExactly(Kind.OPENING_THROW, Kind.OPENING_THROW, Kind.ROLL, Kind.TURN, Kind.TURN);
        assertThat(entries.get(1).dice()).containsExactly(6, 1);
        assertThat(entries.get(1).player()).isNull();
        assertThat(entries.get(2).turnNumber()).isEqualTo(1);
        assertThat(entries.get(2).player()).isEqualTo(Player.WHITE);
        assertThat(entries.get(3).moves()).isEqualTo(moves);
        assertThat(entries.get(3).dice()).isNull();
        assertThat(entries.get(4).moves()).isEmpty();
        assertThat(entries).allSatisfy(e -> assertThat(e.createdAt()).isNotNull());
    }

    @Test
    void logEntriesNeedAnExistingGame() {
        assertThatThrownBy(() -> log.appendRoll(UUID.randomUUID(), 1, Player.WHITE, new Roll(1, 2)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
