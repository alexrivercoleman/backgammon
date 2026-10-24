package com.tavli.engine;

import static com.tavli.engine.Positions.board;
import static com.tavli.engine.Positions.move;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class BoardTest {

    @ParameterizedTest
    @EnumSource(Player.class)
    void startingPositionIsStandardFromEachPlayersPerspective(Player player) {
        Board board = Board.starting();

        assertThat(board.count(player, Board.point(player, 24))).isEqualTo(2);
        assertThat(board.count(player, Board.point(player, 13))).isEqualTo(5);
        assertThat(board.count(player, Board.point(player, 8))).isEqualTo(3);
        assertThat(board.count(player, Board.point(player, 6))).isEqualTo(5);
        assertThat(board.bar(player)).isZero();
        assertThat(board.off(player)).isZero();
        assertThat(board.allHome(player)).isFalse();
        assertThat(board.highestPip(player)).isEqualTo(24);
    }

    @Test
    void startingPositionUsesAbsolutePointsWithOppositeDirections() {
        Board board = Board.starting();

        assertThat(board.points()).containsExactly(
                -2, 0, 0, 0, 0, 5, 0, 3, 0, 0, 0, -5,
                5, 0, 0, 0, -3, 0, -5, 0, 0, 0, 0, 2);
    }

    @ParameterizedTest
    @CsvSource({
            "WHITE, 1, 1",
            "WHITE, 24, 24",
            "BLACK, 1, 24",
            "BLACK, 6, 19",
            "BLACK, 24, 1",
    })
    void pipsMapToAbsolutePoints(Player player, int pip, int point) {
        assertThat(Board.point(player, pip)).isEqualTo(point);
        assertThat(Board.point(player, point)).isEqualTo(pip);
    }

    @Test
    void rejectsWrongNumberOfCheckers() {
        List<Integer> points = Collections.nCopies(24, 0);

        assertThatThrownBy(() -> new Board(points, 0, 0, 14, 15))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("white=14");
    }

    @Test
    void rejectsWrongNumberOfPoints() {
        assertThatThrownBy(() -> new Board(Collections.nCopies(23, 0), 0, 0, 15, 15))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pointsAreImmutable() {
        assertThatThrownBy(() -> Board.starting().points().set(0, 3))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void applyMovesAChecker() {
        Board after = Board.starting().apply(Player.WHITE, move("13/10"));

        assertThat(after.count(Player.WHITE, 13)).isEqualTo(4);
        assertThat(after.count(Player.WHITE, 10)).isEqualTo(1);
    }

    @Test
    void applyHitsABlotAndSendsItToTheBar() {
        Board before = board().white(13, 1).black(10, 1).build();

        Board after = before.apply(Player.WHITE, move("13/10"));

        assertThat(after.count(Player.WHITE, 10)).isEqualTo(1);
        assertThat(after.count(Player.BLACK, 10)).isZero();
        assertThat(after.bar(Player.BLACK)).isEqualTo(1);
    }

    @Test
    void applyEntersFromTheBarAndBearsOff() {
        Board before = board().whiteBar(1).white(3, 1).build();

        Board after = before.apply(Player.WHITE, move("bar/22")).apply(Player.WHITE, move("3/off"));

        assertThat(after.bar(Player.WHITE)).isZero();
        assertThat(after.count(Player.WHITE, 22)).isEqualTo(1);
        assertThat(after.off(Player.WHITE)).isEqualTo(14);
    }

    @Test
    void applyRefusesBlockedPointAndMissingChecker() {
        Board board = board().white(13, 1).black(10, 2).build();

        assertThatThrownBy(() -> board.apply(Player.WHITE, move("13/10"))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> board.apply(Player.WHITE, move("12/10"))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void allHomeRequiresNoCheckersOutsideHomeOrOnBar() {
        assertThat(board().white(6, 3).white(1, 2).build().allHome(Player.WHITE)).isTrue();
        assertThat(board().white(6, 3).white(7, 1).build().allHome(Player.WHITE)).isFalse();
        assertThat(board().white(6, 3).whiteBar(1).build().allHome(Player.WHITE)).isFalse();
    }

    @Test
    void mirrorSwapsColoursAndReflectsPoints() {
        Board white = board().white(24, 2).white(6, 1).whiteBar(1).black(3, 2).build();

        Board black = Positions.mirror(white);

        assertThat(black.count(Player.BLACK, 1)).isEqualTo(2);
        assertThat(black.count(Player.BLACK, 19)).isEqualTo(1);
        assertThat(black.bar(Player.BLACK)).isEqualTo(1);
        assertThat(black.count(Player.WHITE, 22)).isEqualTo(2);
        assertThat(Positions.mirror(black)).isEqualTo(white);
        assertThat(Positions.mirror(Board.starting())).isEqualTo(Board.starting());
    }
}
