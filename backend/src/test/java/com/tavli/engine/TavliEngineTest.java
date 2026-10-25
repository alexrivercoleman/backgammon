package com.tavli.engine;

import static com.tavli.engine.Positions.board;
import static com.tavli.engine.Positions.forPlayer;
import static com.tavli.engine.Positions.moves;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class TavliEngineTest {

    @Test
    void startRollsNothingAndAsksWhiteToThrowFirst() {
        FixedDice dice = new FixedDice();

        GameState state = new TavliEngine(dice).start();

        assertThat(state.phase()).isEqualTo(Phase.OPENING);
        assertThat(state.board()).isEqualTo(Board.starting());
        assertThat(state.toMove()).isEqualTo(Player.WHITE);
        assertThat(state.roll()).isNull();
        assertThat(state.openingDie()).isNull();
    }

    @ParameterizedTest
    @CsvSource({"5, 3, WHITE", "2, 6, BLACK", "1, 2, BLACK", "6, 1, WHITE"})
    void eachPlayerThrowsOneDieAndTheHigherStartsWithBoth(int white, int black, Player first) {
        TavliEngine engine = new TavliEngine(new FixedDice(white, black));

        RollOutcome whiteThrow = engine.roll(engine.start());
        assertThat(whiteThrow.openingThrow()).isNull();
        assertThat(whiteThrow.state().phase()).isEqualTo(Phase.OPENING);
        assertThat(whiteThrow.state().toMove()).isEqualTo(Player.BLACK);
        assertThat(whiteThrow.state().openingDie()).isEqualTo(white);

        RollOutcome blackThrow = engine.roll(whiteThrow.state());
        assertThat(blackThrow.openingThrow()).isEqualTo(new OpeningThrow(white, black));
        GameState state = blackThrow.state();
        assertThat(state.phase()).isEqualTo(Phase.MOVING);
        assertThat(state.toMove()).isEqualTo(first);
        assertThat(state.roll()).isEqualTo(new Roll(white, black));
        assertThat(state.board()).isEqualTo(Board.starting());
    }

    @Test
    void tiedOpeningThrowsAreThrownAgain() {
        FixedDice dice = new FixedDice(4, 4, 3, 6);
        TavliEngine engine = new TavliEngine(dice);

        RollOutcome tie = engine.roll(engine.roll(engine.start()).state());

        assertThat(tie.openingThrow()).isEqualTo(new OpeningThrow(4, 4));
        assertThat(tie.state()).isEqualTo(GameState.opening());

        GameState started = engine.roll(engine.roll(tie.state()).state()).state();
        assertThat(started.toMove()).isEqualTo(Player.BLACK);
        assertThat(started.roll()).isEqualTo(new Roll(3, 6));
        assertThat(dice.exhausted()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void playingATurnLeavesTheOpponentToRoll(Player player) {
        TavliEngine engine = new TavliEngine(new FixedDice());
        GameState state = GameState.moving(Board.starting(), player, new Roll(3, 1));

        GameState next = engine.play(state, moves(player, "8/5 6/5"));

        assertThat(next.phase()).isEqualTo(Phase.ROLLING);
        assertThat(next.toMove()).isEqualTo(player.opponent());
        assertThat(next.roll()).isNull();
        assertThat(next.board().count(player, Board.point(player, 5))).isEqualTo(2);
        assertThat(state.board()).isEqualTo(Board.starting());
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void rollingGivesThePlayerToMoveTwoDice(Player player) {
        TavliEngine engine = new TavliEngine(new FixedDice(5, 2));

        RollOutcome rolled = engine.roll(GameState.awaitingRoll(Board.starting(), player));

        assertThat(rolled.openingThrow()).isNull();
        assertThat(rolled.state()).isEqualTo(GameState.moving(Board.starting(), player, new Roll(5, 2)));
    }

    @Test
    void eachActionIsOnlyAllowedInItsPhase() {
        TavliEngine engine = new TavliEngine(new FixedDice());
        GameState moving = GameState.moving(Board.starting(), Player.WHITE, new Roll(3, 1));
        GameState rolling = GameState.awaitingRoll(Board.starting(), Player.WHITE);

        assertThatThrownBy(() -> engine.roll(moving)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> engine.play(rolling, List.of())).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> engine.plan(engine.start())).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void illegalOrIncompleteTurnsAreRejected(Player player) {
        TavliEngine engine = new TavliEngine(new FixedDice());
        GameState state = GameState.moving(Board.starting(), player, new Roll(6, 5));

        assertThatThrownBy(() -> engine.play(state, moves(player, "24/19 13/7")))
                .isInstanceOf(IllegalTurnException.class);
        assertThatThrownBy(() -> engine.play(state, moves(player, "13/7")))
                .isInstanceOf(IllegalTurnException.class);
        assertThatThrownBy(() -> engine.play(state, List.of()))
                .isInstanceOf(IllegalTurnException.class);
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void aBlockedPlayerPassesWithAnEmptyTurn(Player player) {
        Board whiteView = board().whiteBar(1).white(13, 5)
                .black(19, 2).black(20, 2).black(21, 2).black(22, 2).black(23, 2).black(24, 1)
                .build();
        TavliEngine engine = new TavliEngine(new FixedDice());
        GameState state = GameState.moving(forPlayer(player, whiteView), player, new Roll(6, 5));

        assertThat(engine.plan(state).mustPass()).isTrue();
        GameState next = engine.play(state, List.of());

        assertThat(next.board()).isEqualTo(state.board());
        assertThat(next.toMove()).isEqualTo(player.opponent());
        assertThat(next.phase()).isEqualTo(Phase.ROLLING);
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void bearingOffTheLastCheckerWinsAndEndsTheGame(Player player) {
        TavliEngine engine = new TavliEngine(new FixedDice());
        Board whiteView = board().white(3, 1).white(1, 1).black(13, 15).build();
        GameState state = GameState.moving(forPlayer(player, whiteView), player, new Roll(3, 1));

        GameState over = engine.play(state, moves(player, "3/off 1/off"));

        assertThat(over.isOver()).isTrue();
        assertThat(over.phase()).isEqualTo(Phase.FINISHED);
        assertThat(over.winner()).isEqualTo(player);
        assertThat(over.roll()).isNull();
        assertThat(over.board().off(player)).isEqualTo(Board.CHECKERS_PER_PLAYER);
        assertThatThrownBy(() -> engine.play(over, List.of())).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> engine.roll(over)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void statesMustBeConsistentWithTheirPhase() {
        Board start = Board.starting();
        Board whiteDone = board().black(13, 1).build();

        assertThatThrownBy(() -> new GameState(Phase.MOVING, start, Player.WHITE, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GameState(Phase.ROLLING, start, Player.WHITE, new Roll(1, 2), null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GameState(Phase.OPENING, start, Player.BLACK, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GameState(Phase.OPENING, start, Player.WHITE, null, 3, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GameState.finished(start, Player.WHITE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(GameState.finished(whiteDone, Player.WHITE).isOver()).isTrue();
    }

    @Test
    void randomGamesAlwaysFinishWithAWinner() {
        Random random = new Random(42);
        for (int game = 0; game < 50; game++) {
            TavliEngine engine = new TavliEngine(new RandomDice(random));
            GameState state = engine.start();
            int actions = 0;
            while (!state.isOver()) {
                if (state.phase() == Phase.MOVING) {
                    List<List<Move>> options = new ArrayList<>(engine.plan(state).sequences());
                    state = engine.play(state, options.get(random.nextInt(options.size())));
                } else {
                    state = engine.roll(state).state();
                }
                assertThat(++actions).as("game %d is taking too long", game).isLessThan(5_000);
            }
            Player winner = state.winner();
            assertThat(state.board().off(winner)).isEqualTo(Board.CHECKERS_PER_PLAYER);
            assertThat(state.board().off(winner.opponent())).isLessThan(Board.CHECKERS_PER_PLAYER);
        }
    }
}
