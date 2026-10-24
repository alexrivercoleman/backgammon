package com.tavli.game;

import static com.tavli.engine.Positions.board;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tavli.IntegrationTestConfiguration;
import com.tavli.ScriptedDice;
import com.tavli.engine.Board;
import com.tavli.engine.GameState;
import com.tavli.engine.IllegalTurnException;
import com.tavli.engine.Move;
import com.tavli.engine.Player;
import com.tavli.engine.Roll;
import com.tavli.game.LogEntry.Kind;
import com.tavli.game.persistence.GameRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(IntegrationTestConfiguration.class)
class GameServiceIT {

    @Autowired
    GameService service;

    @Autowired
    GameRepository games;

    @Autowired
    ScriptedDice dice;

    @BeforeEach
    void resetDice() {
        dice.reset();
    }

    @Test
    void createLogsEveryOpeningThrowAndTheFirstRoll() {
        dice.script(4, 4, 2, 2, 3, 6);

        Game game = service.create();

        assertThat(game.turnNumber()).isEqualTo(1);
        assertThat(game.state().board()).isEqualTo(Board.starting());
        assertThat(game.state().toMove()).isEqualTo(Player.BLACK);
        assertThat(game.state().roll()).isEqualTo(new Roll(3, 6));
        assertThat(service.get(game.id())).isEqualTo(game);

        List<LogEntry> log = service.log(game.id());
        assertThat(log).extracting(LogEntry::kind)
                .containsExactly(Kind.OPENING_THROW, Kind.OPENING_THROW, Kind.OPENING_THROW, Kind.ROLL);
        assertThat(log).extracting(LogEntry::dice)
                .containsExactly(List.of(4, 4), List.of(2, 2), List.of(3, 6), List.of(3, 6));
        assertThat(log.get(3).player()).isEqualTo(Player.BLACK);
        assertThat(log.get(3).turnNumber()).isEqualTo(1);
    }

    @Test
    void playingATurnSavesTheStateAndLogsTheTurnAndNextRoll() {
        dice.script(3, 1, 5, 2);
        Game game = service.create();
        List<Move> turn = List.of(new Move(8, 5, 3), new Move(6, 5, 1));

        Game next = service.play(game.id(), 1, turn);

        assertThat(next.turnNumber()).isEqualTo(2);
        assertThat(next.state().toMove()).isEqualTo(Player.BLACK);
        assertThat(next.state().roll()).isEqualTo(new Roll(5, 2));
        assertThat(service.get(game.id())).isEqualTo(next);

        List<LogEntry> log = service.log(game.id());
        assertThat(log).extracting(LogEntry::kind).endsWith(Kind.ROLL, Kind.TURN, Kind.ROLL);
        LogEntry turnEntry = log.get(log.size() - 2);
        assertThat(turnEntry.moves()).isEqualTo(turn);
        assertThat(turnEntry.player()).isEqualTo(Player.WHITE);
        assertThat(turnEntry.turnNumber()).isEqualTo(1);
        assertThat(log.getLast().player()).isEqualTo(Player.BLACK);
        assertThat(log.getLast().turnNumber()).isEqualTo(2);
    }

    @Test
    void illegalTurnsChangeNothing() {
        dice.script(6, 5);
        Game game = service.create();
        List<LogEntry> logBefore = service.log(game.id());

        assertThatThrownBy(() -> service.play(game.id(), 1, List.of(new Move(13, 7, 6))))
                .isInstanceOf(IllegalTurnException.class);

        assertThat(service.get(game.id())).isEqualTo(game);
        assertThat(service.log(game.id())).isEqualTo(logBefore);
    }

    @Test
    void staleTurnNumbersAreRejected() {
        dice.script(3, 1, 5, 2);
        Game game = service.create();
        List<Move> turn = List.of(new Move(8, 5, 3), new Move(6, 5, 1));
        service.play(game.id(), 1, turn);
        int logSize = service.log(game.id()).size();

        assertThatThrownBy(() -> service.play(game.id(), 1, turn)).isInstanceOf(StaleTurnException.class);

        assertThat(service.get(game.id()).turnNumber()).isEqualTo(2);
        assertThat(service.log(game.id())).hasSize(logSize);
    }

    @Test
    void concurrentSubmitsOfTheSameTurnApplyOnlyOnce() throws Exception {
        dice.script(3, 1).thenSeeded(1);
        Game game = service.create();
        List<Move> turn = List.of(new Move(8, 5, 3), new Move(6, 5, 1));
        CountDownLatch start = new CountDownLatch(1);

        List<Callable<Object>> submits = List.of(submitAfter(start, game.id(), turn), submitAfter(start, game.id(), turn));
        List<Future<Object>> results;
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            List<Future<Object>> futures = submits.stream().map(pool::submit).toList();
            start.countDown();
            results = futures;
            for (Future<Object> f : futures) {
                f.get(30, TimeUnit.SECONDS);
            }
        }

        assertThat(results).extracting(Future::resultNow)
                .filteredOn(r -> r instanceof Game).hasSize(1);
        assertThat(results).extracting(Future::resultNow)
                .filteredOn(r -> r instanceof StaleTurnException).hasSize(1);
        assertThat(service.get(game.id()).turnNumber()).isEqualTo(2);
        assertThat(service.log(game.id())).filteredOn(e -> e.kind() == Kind.TURN).hasSize(1);
    }

    private Callable<Object> submitAfter(CountDownLatch start, UUID id, List<Move> turn) {
        return () -> {
            start.await();
            try {
                return service.play(id, 1, turn);
            } catch (StaleTurnException e) {
                return e;
            }
        };
    }

    @Test
    void unknownGamesAreReported() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.get(id)).isInstanceOf(GameNotFoundException.class);
        assertThatThrownBy(() -> service.log(id)).isInstanceOf(GameNotFoundException.class);
        assertThatThrownBy(() -> service.play(id, 1, List.of())).isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void aBlockedPlayerPassesWithAnEmptyTurn() {
        Board blocked = board().whiteBar(1).white(13, 5)
                .black(19, 2).black(20, 2).black(21, 2).black(22, 2).black(23, 2).black(24, 2).build();
        Game game = saved(new GameState(blocked, Player.WHITE, new Roll(6, 5), null), 7);
        dice.script(2, 1);

        assertThat(service.currentTurn(game)).hasValueSatisfying(plan -> assertThat(plan.mustPass()).isTrue());
        Game next = service.play(game.id(), 7, List.of());

        assertThat(next.state().board()).isEqualTo(blocked);
        assertThat(next.state().toMove()).isEqualTo(Player.BLACK);
        assertThat(service.log(game.id()).getFirst().moves()).isEmpty();
    }

    @Test
    void bearingOffTheLastCheckerFinishesTheGame() {
        Board nearlyDone = board().white(3, 1).white(1, 1).black(13, 15).build();
        Game game = saved(new GameState(nearlyDone, Player.WHITE, new Roll(3, 1), null), 30);

        Game finished = service.play(game.id(), 30, List.of(new Move(3, Move.OFF, 3), new Move(1, Move.OFF, 1)));

        assertThat(finished.state().isOver()).isTrue();
        assertThat(finished.state().winner()).isEqualTo(Player.WHITE);
        assertThat(service.get(game.id())).isEqualTo(finished);
        assertThat(service.currentTurn(finished)).isEmpty();
        // The winning turn is logged with no roll after it.
        assertThat(service.log(game.id())).extracting(LogEntry::kind).containsExactly(Kind.TURN);
        assertThatThrownBy(() -> service.play(game.id(), 31, List.of())).isInstanceOf(GameFinishedException.class);
    }

    private Game saved(GameState state, int turnNumber) {
        Game game = new Game(UUID.randomUUID(), turnNumber, state);
        games.insert(game);
        return game;
    }
}
