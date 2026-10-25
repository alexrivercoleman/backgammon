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
import com.tavli.engine.Phase;
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

    private static final List<Move> EIGHT_FIVE_SIX_FIVE = List.of(new Move(8, 5, 3), new Move(6, 5, 1));

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

    /** Creates a game and throws the opening (White's die, then Black's) so the starter is ready to move. */
    private Game started(int white, int black) {
        dice.script(white, black);
        Game game = service.create();
        service.roll(game.id(), 1, Player.WHITE);
        return service.roll(game.id(), 1, Player.BLACK);
    }

    @Test
    void createRollsNothingAndWaitsForWhitesOpeningDie() {
        Game game = service.create();

        assertThat(game.turnNumber()).isEqualTo(1);
        assertThat(game.state()).isEqualTo(GameState.opening());
        assertThat(service.get(game.id())).isEqualTo(game);
        assertThat(service.log(game.id())).isEmpty();
    }

    @Test
    void openingThrowsAreLoggedOncePerRoundUntilSomeoneStarts() {
        dice.script(4, 4, 3, 6);
        Game game = service.create();

        Game whiteThrew = service.roll(game.id(), 1, Player.WHITE);
        assertThat(whiteThrew.state().openingDie()).isEqualTo(4);
        assertThat(whiteThrew.state().toMove()).isEqualTo(Player.BLACK);
        assertThat(service.log(game.id())).isEmpty();

        Game tied = service.roll(game.id(), 1, Player.BLACK);
        assertThat(tied.state()).isEqualTo(GameState.opening());
        assertThat(service.openingThrows(tied)).extracting(t -> List.of(t.white(), t.black()))
                .containsExactly(List.of(4, 4));

        service.roll(game.id(), 1, Player.WHITE);
        Game started = service.roll(game.id(), 1, Player.BLACK);
        assertThat(started.state()).isEqualTo(GameState.moving(Board.starting(), Player.BLACK, new Roll(3, 6)));
        assertThat(started.turnNumber()).isEqualTo(1);

        List<LogEntry> log = service.log(game.id());
        assertThat(log).extracting(LogEntry::kind).containsExactly(Kind.OPENING_THROW, Kind.OPENING_THROW, Kind.ROLL);
        assertThat(log).extracting(LogEntry::dice).containsExactly(List.of(4, 4), List.of(3, 6), List.of(3, 6));
        assertThat(log.get(2).player()).isEqualTo(Player.BLACK);
        assertThat(log.get(2).turnNumber()).isEqualTo(1);
    }

    @Test
    void playingATurnLeavesTheOpponentToRollTheirOwnDice() {
        Game game = started(3, 1);

        Game next = service.play(game.id(), 1, EIGHT_FIVE_SIX_FIVE);

        assertThat(next.turnNumber()).isEqualTo(2);
        assertThat(next.state().phase()).isEqualTo(Phase.ROLLING);
        assertThat(next.state().toMove()).isEqualTo(Player.BLACK);
        assertThat(service.get(game.id())).isEqualTo(next);
        assertThat(service.log(game.id())).extracting(LogEntry::kind).endsWith(Kind.ROLL, Kind.TURN);

        dice.script(5, 2);
        Game rolled = service.roll(game.id(), 2, Player.BLACK);

        assertThat(rolled.state().roll()).isEqualTo(new Roll(5, 2));
        LogEntry roll = service.log(game.id()).getLast();
        assertThat(roll.kind()).isEqualTo(Kind.ROLL);
        assertThat(roll.player()).isEqualTo(Player.BLACK);
        assertThat(roll.turnNumber()).isEqualTo(2);
        assertThat(roll.dice()).containsExactly(5, 2);
    }

    @Test
    void actionsOutOfTurnAreRefused() {
        Game game = service.create();

        assertThatThrownBy(() -> service.roll(game.id(), 1, Player.BLACK))
                .isInstanceOf(ActionNotAllowedException.class).hasMessageContaining("WHITE's roll");
        assertThatThrownBy(() -> service.play(game.id(), 1, List.of()))
                .isInstanceOf(ActionNotAllowedException.class).hasMessageContaining("must roll");

        Game moving = started(3, 1);
        assertThatThrownBy(() -> service.roll(moving.id(), 1, Player.WHITE))
                .isInstanceOf(ActionNotAllowedException.class).hasMessageContaining("already rolled");
    }

    @Test
    void illegalTurnsChangeNothing() {
        Game game = started(6, 5);
        List<LogEntry> logBefore = service.log(game.id());

        assertThatThrownBy(() -> service.play(game.id(), 1, List.of(new Move(13, 7, 6))))
                .isInstanceOf(IllegalTurnException.class);

        assertThat(service.get(game.id())).isEqualTo(game);
        assertThat(service.log(game.id())).isEqualTo(logBefore);
    }

    @Test
    void staleTurnNumbersAreRejected() {
        Game game = started(3, 1);
        service.play(game.id(), 1, EIGHT_FIVE_SIX_FIVE);
        int logSize = service.log(game.id()).size();

        assertThatThrownBy(() -> service.play(game.id(), 1, EIGHT_FIVE_SIX_FIVE)).isInstanceOf(StaleTurnException.class);
        assertThatThrownBy(() -> service.roll(game.id(), 1, Player.BLACK)).isInstanceOf(StaleTurnException.class);

        assertThat(service.get(game.id()).turnNumber()).isEqualTo(2);
        assertThat(service.log(game.id())).hasSize(logSize);
    }

    @Test
    void concurrentRollsForTheSameTurnRollOnlyOnce() throws Exception {
        Game game = started(3, 1);
        service.play(game.id(), 1, EIGHT_FIVE_SIX_FIVE);
        dice.thenSeeded(1);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<Object>> results;
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            results = List.of(pool.submit(rollAfter(start, game.id())), pool.submit(rollAfter(start, game.id())));
            start.countDown();
            for (Future<Object> f : results) {
                f.get(30, TimeUnit.SECONDS);
            }
        }

        assertThat(results).extracting(Future::resultNow).filteredOn(r -> r instanceof Game).hasSize(1);
        assertThat(results).extracting(Future::resultNow)
                .filteredOn(r -> r instanceof ActionNotAllowedException).hasSize(1);
        assertThat(service.log(game.id())).filteredOn(e -> e.kind() == Kind.ROLL && e.turnNumber() == 2).hasSize(1);
    }

    private Callable<Object> rollAfter(CountDownLatch start, UUID id) {
        return () -> {
            start.await();
            try {
                return service.roll(id, 2, Player.BLACK);
            } catch (ActionNotAllowedException e) {
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
        assertThatThrownBy(() -> service.roll(id, 1, Player.WHITE)).isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void aBlockedPlayerPassesWithAnEmptyTurn() {
        Board blocked = board().whiteBar(1).white(13, 5)
                .black(19, 2).black(20, 2).black(21, 2).black(22, 2).black(23, 2).black(24, 2).build();
        Game game = saved(GameState.moving(blocked, Player.WHITE, new Roll(6, 5)), 7);

        assertThat(service.currentTurn(game)).hasValueSatisfying(plan -> assertThat(plan.mustPass()).isTrue());
        Game next = service.play(game.id(), 7, List.of());

        assertThat(next.state()).isEqualTo(GameState.awaitingRoll(blocked, Player.BLACK));
        assertThat(service.log(game.id()).getFirst().moves()).isEmpty();
    }

    @Test
    void bearingOffTheLastCheckerFinishesTheGame() {
        Board nearlyDone = board().white(3, 1).white(1, 1).black(13, 15).build();
        Game game = saved(GameState.moving(nearlyDone, Player.WHITE, new Roll(3, 1)), 30);

        Game finished = service.play(game.id(), 30, List.of(new Move(3, Move.OFF, 3), new Move(1, Move.OFF, 1)));

        assertThat(finished.state().isOver()).isTrue();
        assertThat(finished.state().winner()).isEqualTo(Player.WHITE);
        assertThat(service.get(game.id())).isEqualTo(finished);
        assertThat(service.currentTurn(finished)).isEmpty();
        assertThat(service.log(game.id())).extracting(LogEntry::kind).containsExactly(Kind.TURN);
        assertThatThrownBy(() -> service.play(game.id(), 31, List.of())).isInstanceOf(GameFinishedException.class);
        assertThatThrownBy(() -> service.roll(game.id(), 31, Player.BLACK)).isInstanceOf(GameFinishedException.class);
    }

    private Game saved(GameState state, int turnNumber) {
        Game game = new Game(UUID.randomUUID(), turnNumber, state);
        games.insert(game);
        return game;
    }
}
