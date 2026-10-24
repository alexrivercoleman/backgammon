package com.tavli.engine;

import static com.tavli.engine.Positions.board;
import static com.tavli.engine.Positions.forPlayer;
import static com.tavli.engine.Positions.moves;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Whole-turn legality. Every scenario is written for WHITE and checked for both players. */
class TurnPlanTest {

    private static TurnPlan plan(Player player, Board whiteView, int first, int second) {
        return TurnPlan.of(forPlayer(player, whiteView), player, new Roll(first, second));
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void bothOrdersOfTheSameMovesAreLegalAndShareAnOutcome(Player player) {
        TurnPlan plan = plan(player, Board.starting(), 3, 1);

        assertThat(plan.isLegal(moves(player, "8/5 6/5"))).isTrue();
        assertThat(plan.isLegal(moves(player, "6/5 8/5"))).isTrue();
        assertThat(plan.resultOf(moves(player, "8/5 6/5"))).isEqualTo(plan.resultOf(moves(player, "6/5 8/5")));
        assertThat(plan.outcomes()).hasSizeLessThan(plan.sequences().size());
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void oneCheckerCanUseBothDice(Player player) {
        TurnPlan plan = plan(player, Board.starting(), 6, 5);

        assertThat(plan.isLegal(moves(player, "24/18 18/13"))).isTrue();
        assertThat(plan.nextMoves(moves(player, "24/18"))).contains(forPlayer(player, Positions.move("18/13")));
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void everyLegalTurnUsesBothDiceWhenPossible(Player player) {
        TurnPlan plan = plan(player, Board.starting(), 6, 5);

        assertThat(plan.sequences()).allSatisfy(turn -> assertThat(turn).hasSize(2));
        assertThat(plan.isLegal(moves(player, "13/7"))).isFalse();
        assertThat(plan.isLegal(List.of())).isFalse();
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void hittingABlotDuringATurnSendsItToTheBar(Player player) {
        Board whiteView = board().white(13, 2).black(10, 1).black(1, 2).build();

        Board after = plan(player, whiteView, 3, 2).resultOf(moves(player, "13/10 10/8"));

        assertThat(after.bar(player.opponent())).isEqualTo(1);
        assertThat(after.count(player, Board.point(player, 8))).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void barCheckerMustEnterFirstThenTheOtherDieIsFree(Player player) {
        Board whiteView = board().whiteBar(1).white(13, 2).build();
        TurnPlan plan = plan(player, whiteView, 3, 5);

        assertThat(plan.nextMoves(List.of()))
                .containsExactlyInAnyOrderElementsOf(moves(player, "bar/22 bar/20"));
        assertThat(plan.nextMoves(moves(player, "bar/22")))
                .containsExactlyInAnyOrderElementsOf(moves(player, "22/17 13/8"));
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void withTwoOnTheBarAndOneEntryBlockedOnlyOneCheckerEnters(Player player) {
        Board whiteView = board().whiteBar(2).white(13, 2).black(22, 2).build();
        TurnPlan plan = plan(player, whiteView, 3, 5);

        assertThat(plan.sequences()).containsExactly(moves(player, "bar/20"));
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void fullyBlockedEntryMeansThePlayerMustPass(Player player) {
        Board whiteView = board().whiteBar(1).white(13, 5)
                .black(19, 2).black(20, 2).black(21, 2).black(22, 2).black(23, 2).black(24, 2)
                .build();
        TurnPlan plan = plan(player, whiteView, 6, 6);

        assertThat(plan.mustPass()).isTrue();
        assertThat(plan.sequences()).containsExactly(List.of());
        assertThat(plan.nextMoves(List.of())).isEmpty();
        assertThat(plan.resultOf(List.of())).isEqualTo(plan.start());
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void noLegalMovesMeansThePlayerMustPass(Player player) {
        Board whiteView = board().white(13, 1).black(7, 2).black(8, 2).build();

        assertThat(plan(player, whiteView, 6, 5).mustPass()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void doublesPlayFourMoves(Player player) {
        TurnPlan plan = plan(player, Board.starting(), 6, 6);

        assertThat(plan.isLegal(moves(player, "24/18 24/18 13/7 13/7"))).isTrue();
        assertThat(plan.isLegal(moves(player, "13/7 24/18 13/7 24/18"))).isTrue();
        assertThat(plan.sequences()).allSatisfy(turn -> assertThat(turn).hasSize(4));
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void doublesPlayAsManyMovesAsPossible(Player player) {
        // One checker on 13 and 7, 5 and 3 blocked: only 13/11/9 is possible.
        Board whiteView = board().white(13, 1).black(7, 2).black(5, 2).black(3, 2).build();
        TurnPlan plan = plan(player, whiteView, 2, 2);

        assertThat(plan.sequences()).containsExactly(moves(player, "13/11 11/9"));
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void aMoveThatWouldStrandTheOtherDieIsNotOffered(Player player) {
        // 13/9 is playable on its own, but afterwards no 2 can be played because the 7-point is blocked.
        Board whiteView = board().white(13, 1).white(9, 1).black(7, 2).build();
        TurnPlan plan = plan(player, whiteView, 4, 2);

        assertThat(MoveGenerator.singleMoves(forPlayer(player, whiteView), player, 4))
                .contains(forPlayer(player, Positions.move("13/9")));
        assertThat(plan.nextMoves(List.of()))
                .containsExactlyInAnyOrderElementsOf(moves(player, "9/5 13/11"));
        assertThat(plan.isLegal(moves(player, "13/9"))).isFalse();
        assertThat(plan.sequences()).containsExactlyInAnyOrder(
                moves(player, "9/5 13/11"), moves(player, "13/11 9/5"), moves(player, "9/5 5/3"));
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void whenOnlyOneDieCanBeUsedItMustBeTheHigher(Player player) {
        // Either die alone works, but 13/2 is blocked, so only one can be used.
        Board whiteView = board().white(13, 1).black(2, 2).build();
        TurnPlan plan = plan(player, whiteView, 5, 6);

        assertThat(plan.sequences()).containsExactly(moves(player, "13/7"));
        assertThat(plan.isLegal(moves(player, "13/8"))).isFalse();
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void whenOnlyTheLowerDieCanBeUsedItIsPlayed(Player player) {
        Board whiteView = board().white(13, 1).black(7, 2).black(2, 2).build();

        assertThat(plan(player, whiteView, 5, 6).sequences()).containsExactly(moves(player, "13/8"));
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void bearsOffExactly(Player player) {
        Board whiteView = board().white(6, 1).white(4, 1).build();

        Board after = plan(player, whiteView, 6, 4).resultOf(moves(player, "6/off 4/off"));

        assertThat(after.off(player)).isEqualTo(Board.CHECKERS_PER_PLAYER);
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void bearsOffWithAHigherDieFromTheHighestPoint(Player player) {
        Board whiteView = board().white(5, 1).white(3, 1).white(1, 1).build();
        TurnPlan plan = plan(player, whiteView, 6, 4);

        assertThat(plan.isLegal(moves(player, "5/off:6 3/off:4"))).isTrue();
        assertThat(plan.isLegal(moves(player, "5/1 3/off:6"))).isTrue();
        assertThat(plan.nextMoves(List.of())).doesNotContain(forPlayer(player, Positions.move("3/off:6")));
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void lastCheckerHomeCanBearOffWithTheSecondDie(Player player) {
        Board whiteView = board().white(8, 1).white(3, 2).build();

        assertThat(plan(player, whiteView, 2, 6).isLegal(moves(player, "8/6 6/off"))).isTrue();
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void bearingOffTheLastCheckerWinsEvenWithADieUnused(Player player) {
        Board whiteView = board().white(2, 1).build();
        TurnPlan plan = plan(player, whiteView, 3, 1);

        assertThat(plan.isLegal(moves(player, "2/off:3"))).isTrue();
        assertThat(plan.isLegal(moves(player, "2/1 1/off:3"))).isTrue();
        assertThat(plan.isLegal(moves(player, "2/1"))).isFalse();
        assertThat(plan.resultOf(moves(player, "2/off:3")).off(player)).isEqualTo(Board.CHECKERS_PER_PLAYER);
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void doublesStopOnceTheLastCheckerIsOff(Player player) {
        Board whiteView = board().white(1, 2).build();

        assertThat(plan(player, whiteView, 4, 4).sequences())
                .containsExactly(moves(player, "1/off:4 1/off:4"));
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void illegalPrefixesAreRejected(Player player) {
        TurnPlan plan = plan(player, Board.starting(), 6, 5);

        assertThatThrownBy(() -> plan.nextMoves(moves(player, "24/19")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> plan.resultOf(moves(player, "24/18")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(plan.isLegal(moves(player, "24/18 18/13 13/8"))).isFalse();
    }
}
