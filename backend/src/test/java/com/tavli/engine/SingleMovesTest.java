package com.tavli.engine;

import static com.tavli.engine.Positions.board;
import static com.tavli.engine.Positions.forPlayer;
import static com.tavli.engine.Positions.moves;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Single-die move generation. Every scenario is written for WHITE and checked for both players. */
class SingleMovesTest {

    private static List<Move> generate(Player player, Board whiteView, int die) {
        return MoveGenerator.singleMoves(forPlayer(player, whiteView), player, die);
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void fromStartWithASixEveryCheckerOutsideHomeCanMove(Player player) {
        assertThat(generate(player, Board.starting(), 6))
                .containsExactlyInAnyOrderElementsOf(moves(player, "24/18 13/7 8/2"));
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void pointsHeldByTwoOrMoreOpponentsAreBlocked(Player player) {
        // 24/19 lands on the opponent's 5-stack and 6/1 on their 2-stack.
        assertThat(generate(player, Board.starting(), 5))
                .containsExactlyInAnyOrderElementsOf(moves(player, "13/8 8/3"));
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void aBlotCanBeHitAndOwnPointsCanBeStacked(Player player) {
        Board whiteView = board().white(13, 2).white(10, 1).black(9, 1).build();

        assertThat(generate(player, whiteView, 3)).containsExactlyInAnyOrderElementsOf(moves(player, "13/10 10/7"));
        assertThat(generate(player, whiteView, 4)).containsExactlyInAnyOrderElementsOf(moves(player, "13/9 10/6"));

        Board afterHit = forPlayer(player, whiteView).apply(player, forPlayer(player, Positions.move("13/9")));
        assertThat(afterHit.bar(player.opponent())).isEqualTo(1);
        assertThat(afterHit.count(player, Board.point(player, 9))).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void checkersOnTheBarMustEnterBeforeAnythingElseMoves(Player player) {
        Board whiteView = board().whiteBar(1).white(13, 5).build();

        assertThat(generate(player, whiteView, 3)).containsExactlyElementsOf(moves(player, "bar/22"));
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void entryCanHitABlot(Player player) {
        Board whiteView = board().whiteBar(1).black(20, 1).build();

        assertThat(generate(player, whiteView, 5)).containsExactlyElementsOf(moves(player, "bar/20"));
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void blockedEntryPointLeavesNoMoveEvenIfOtherCheckersCouldMove(Player player) {
        Board whiteView = board().whiteBar(1).white(13, 5).black(22, 2).build();

        assertThat(generate(player, whiteView, 3)).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void fullyBlockedHomeBoardAllowsNoEntryWithAnyDie(Player player) {
        Board whiteView = board().whiteBar(1).white(13, 5)
                .black(19, 2).black(20, 2).black(21, 2).black(22, 2).black(23, 2).black(24, 2)
                .build();

        for (int die = 1; die <= 6; die++) {
            assertThat(generate(player, whiteView, die)).as("die %d", die).isEmpty();
        }
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void cannotBearOffBeforeAllCheckersAreHome(Player player) {
        Board whiteView = board().white(7, 1).white(3, 2).build();

        assertThat(generate(player, whiteView, 3)).containsExactlyInAnyOrderElementsOf(moves(player, "7/4"));
    }
}
