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

    @ParameterizedTest
    @CsvSource({"5, 3, WHITE", "2, 6, BLACK", "1, 2, BLACK", "6, 1, WHITE"})
    void higherOpeningThrowMovesFirstWithBothDice(int white, int black, Player first) {
        Opening opening = new TavliEngine(new FixedDice(white, black)).start();

        GameState state = opening.state();
        assertThat(state.board()).isEqualTo(Board.starting());
        assertThat(state.toMove()).isEqualTo(first);
        assertThat(state.roll()).isEqualTo(new Roll(white, black));
        assertThat(state.isOver()).isFalse();
        assertThat(opening.throwsMade()).containsExactly(new Opening.Throw(white, black));
    }

    @Test
    void tiedOpeningThrowsAreRerolled() {
        FixedDice dice = new FixedDice(4, 4, 2, 2, 3, 6);

        Opening opening = new TavliEngine(dice).start();

        assertThat(opening.throwsMade()).containsExactly(
                new Opening.Throw(4, 4), new Opening.Throw(2, 2), new Opening.Throw(3, 6));
        assertThat(opening.state().toMove()).isEqualTo(Player.BLACK);
        assertThat(opening.state().roll()).isEqualTo(new Roll(3, 6));
        assertThat(dice.exhausted()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void playingATurnPassesToTheOpponentWithAFreshRoll(Player player) {
        TavliEngine engine = new TavliEngine(new FixedDice(4, 2));
        GameState state = new GameState(Board.starting(), player, new Roll(3, 1), null);

        GameState next = engine.play(state, moves(player, "8/5 6/5"));

        assertThat(next.toMove()).isEqualTo(player.opponent());
        assertThat(next.roll()).isEqualTo(new Roll(4, 2));
        assertThat(next.board().count(player, Board.point(player, 5))).isEqualTo(2);
        assertThat(state.board()).isEqualTo(Board.starting());
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void illegalOrIncompleteTurnsAreRejected(Player player) {
        TavliEngine engine = new TavliEngine(new FixedDice());
        GameState state = new GameState(Board.starting(), player, new Roll(6, 5), null);

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
        TavliEngine engine = new TavliEngine(new FixedDice(1, 2));
        GameState state = new GameState(forPlayer(player, whiteView), player, new Roll(6, 5), null);

        assertThat(engine.plan(state).mustPass()).isTrue();
        GameState next = engine.play(state, List.of());

        assertThat(next.board()).isEqualTo(state.board());
        assertThat(next.toMove()).isEqualTo(player.opponent());
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void bearingOffTheLastCheckerWinsAndEndsTheGame(Player player) {
        FixedDice dice = new FixedDice();
        TavliEngine engine = new TavliEngine(dice);
        Board whiteView = board().white(3, 1).white(1, 1).black(13, 15).build();
        GameState state = new GameState(forPlayer(player, whiteView), player, new Roll(3, 1), null);

        GameState over = engine.play(state, moves(player, "3/off 1/off"));

        assertThat(over.isOver()).isTrue();
        assertThat(over.winner()).isEqualTo(player);
        assertThat(over.roll()).isNull();
        assertThat(over.board().off(player)).isEqualTo(Board.CHECKERS_PER_PLAYER);
        assertThatThrownBy(() -> engine.play(over, List.of())).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> engine.plan(over)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void finishedStatesMustBeConsistent() {
        Board board = board().white(1, 1).build();

        assertThatThrownBy(() -> new GameState(board, Player.WHITE, null, Player.WHITE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GameState(board, Player.WHITE, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void randomGamesAlwaysFinishWithAWinner() {
        Random random = new Random(42);
        for (int game = 0; game < 50; game++) {
            TavliEngine engine = new TavliEngine(new RandomDice(random));
            GameState state = engine.start().state();
            int turns = 0;
            while (!state.isOver()) {
                List<List<Move>> options = new ArrayList<>(engine.plan(state).sequences());
                state = engine.play(state, options.get(random.nextInt(options.size())));
                assertThat(++turns).as("game %d is taking too long", game).isLessThan(2_000);
            }
            Player winner = state.winner();
            assertThat(state.board().off(winner)).isEqualTo(Board.CHECKERS_PER_PLAYER);
            assertThat(state.board().off(winner.opponent())).isLessThan(Board.CHECKERS_PER_PLAYER);
        }
    }
}
