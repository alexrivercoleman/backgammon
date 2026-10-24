package com.tavli.engine;

import static com.tavli.engine.Positions.board;
import static com.tavli.engine.Positions.forPlayer;
import static com.tavli.engine.Positions.moves;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** The public node/edge view of a {@link TurnPlan}. */
class TurnGraphTest {

    private static TurnPlan plan(Player player, Board whiteView, int first, int second) {
        return TurnPlan.of(forPlayer(player, whiteView), player, new Roll(first, second));
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void nodesAreNumberedFromTheRootAndEdgesPointAtExistingNodes(Player player) {
        List<TurnNode> nodes = plan(player, Board.starting(), 6, 6).nodes();

        assertThat(nodes.getFirst().board()).isEqualTo(Board.starting());
        assertThat(nodes.getFirst().unusedDice()).containsExactly(6, 6, 6, 6);
        for (int i = 0; i < nodes.size(); i++) {
            TurnNode node = nodes.get(i);
            assertThat(node.id()).isEqualTo(i);
            assertThat(node.edges()).allSatisfy(e -> assertThat(e.next()).isBetween(1, nodes.size() - 1));
            assertThat(node.finished()).isEqualTo(node.edges().isEmpty());
        }
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void walkingTheGraphMatchesThePlan(Player player) {
        TurnPlan plan = plan(player, Board.starting(), 3, 1);
        List<TurnNode> nodes = plan.nodes();

        List<List<Move>> finishedPaths = new ArrayList<>();
        walk(plan, nodes, nodes.getFirst(), new ArrayList<>(), finishedPaths);

        assertThat(finishedPaths).containsExactlyInAnyOrderElementsOf(plan.sequences());
    }

    private static void walk(TurnPlan plan, List<TurnNode> nodes, TurnNode node, List<Move> path,
                             List<List<Move>> finished) {
        assertThat(node.board()).isEqualTo(boardAfter(plan, path));
        assertThat(node.unusedDice()).hasSize(plan.roll().dice().size() - path.size());
        if (node.finished()) {
            assertThat(plan.isLegal(path)).isTrue();
            finished.add(List.copyOf(path));
        }
        for (TurnEdge edge : node.edges()) {
            path.add(edge.move());
            walk(plan, nodes, nodes.get(edge.next()), path, finished);
            path.removeLast();
        }
    }

    private static Board boardAfter(TurnPlan plan, List<Move> path) {
        Board board = plan.start();
        for (Move move : path) {
            board = board.apply(plan.player(), move);
        }
        return board;
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void transposedMovesShareANode(Player player) {
        List<TurnNode> nodes = plan(player, Board.starting(), 3, 1).nodes();

        int viaEightFirst = follow(nodes, moves(player, "8/5 6/5"));
        int viaSixFirst = follow(nodes, moves(player, "6/5 8/5"));

        assertThat(viaEightFirst).isEqualTo(viaSixFirst);
    }

    private static int follow(List<TurnNode> nodes, List<Move> path) {
        int id = 0;
        for (Move move : path) {
            id = nodes.get(id).edges().stream().filter(e -> e.move().equals(move)).findFirst().orElseThrow().next();
        }
        return id;
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void hitIsFlaggedOnlyOnMovesThatHitABlot(Player player) {
        Board whiteView = board().white(13, 2).black(10, 1).black(9, 2).black(1, 2).build();
        List<TurnNode> nodes = plan(player, whiteView, 3, 2).nodes();

        assertThat(nodes.getFirst().edges())
                .filteredOn(TurnEdge::hit)
                .extracting(TurnEdge::move)
                .containsExactlyElementsOf(moves(player, "13/10"));
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void aPassIsASingleFinishedNode(Player player) {
        Board whiteView = board().white(13, 1).black(7, 2).black(8, 2).build();

        List<TurnNode> nodes = plan(player, whiteView, 6, 5).nodes();

        assertThat(nodes).hasSize(1);
        assertThat(nodes.getFirst().finished()).isTrue();
        assertThat(nodes.getFirst().edges()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(Player.class)
    void idsAreStableAcrossRebuilds(Player player) {
        assertThat(plan(player, Board.starting(), 5, 5).nodes())
                .isEqualTo(plan(player, Board.starting(), 5, 5).nodes());
    }
}
