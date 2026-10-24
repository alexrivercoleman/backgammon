package com.tavli.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Cross-checks {@link TurnPlan}'s memoised, pruned graph against a brute-force enumeration of every move order
 * with the forced-move rules applied directly to the resulting list of sequences.
 */
class TurnPlanOracleTest {

    private static final int POSITIONS = 100;

    @Test
    void graphMatchesBruteForceOnRandomPositions() {
        Random random = new Random(20261024);
        int checked = 0;
        for (int i = 0; i < POSITIONS; i++) {
            Board board = randomBoard(random);
            for (Player player : Player.values()) {
                for (int a = 1; a <= 6; a++) {
                    for (int b = a; b <= 6; b++) {
                        Roll roll = new Roll(a, b);
                        assertThat(TurnPlan.of(board, player, roll).sequences())
                                .as("%s to play %s on %s", player, roll, board)
                                .isEqualTo(bruteForce(board, player, roll));
                        checked++;
                    }
                }
            }
        }
        assertThat(checked).isEqualTo(POSITIONS * 2 * 21);
    }

    private static Set<List<Move>> bruteForce(Board board, Player player, Roll roll) {
        List<List<Move>> leaves = new ArrayList<>();
        enumerate(board, player, roll.dice(), new ArrayList<>(), leaves);

        int max = leaves.stream().mapToInt(List::size).max().orElse(0);
        List<List<Move>> longest = leaves.stream().filter(s -> s.size() == max).toList();
        if (max == 1 && !roll.isDouble()) {
            List<List<Move>> higher = longest.stream().filter(s -> s.getFirst().die() == roll.higher()).toList();
            if (!higher.isEmpty()) {
                longest = higher;
            }
        }
        Set<List<Move>> legal = new HashSet<>(longest);
        for (List<Move> leaf : leaves) {
            if (replay(board, player, leaf).off(player) == Board.CHECKERS_PER_PLAYER) {
                legal.add(leaf);
            }
        }
        return legal;
    }

    private static void enumerate(Board board, Player player, List<Integer> unused, List<Move> path,
                                  List<List<Move>> leaves) {
        boolean moved = false;
        for (int die : new LinkedHashSet<>(unused)) {
            List<Integer> rest = new ArrayList<>(unused);
            rest.remove(Integer.valueOf(die));
            for (Move move : MoveGenerator.singleMoves(board, player, die)) {
                moved = true;
                path.add(move);
                enumerate(board.apply(player, move), player, rest, path, leaves);
                path.removeLast();
            }
        }
        if (!moved) {
            leaves.add(List.copyOf(path));
        }
    }

    private static Board replay(Board board, Player player, List<Move> moves) {
        for (Move move : moves) {
            board = board.apply(player, move);
        }
        return board;
    }

    /** Random legal layout; about a third of the time a player's checkers are all in their home board. */
    private static Board randomBoard(Random random) {
        int[] points = new int[Board.POINTS];
        int[] bar = new int[2];
        int[] off = new int[2];
        for (Player player : Player.values()) {
            int sign = player == Player.WHITE ? 1 : -1;
            boolean homeOnly = random.nextInt(3) == 0;
            off[player.ordinal()] = random.nextInt(4) == 0 ? random.nextInt(14) : 0;
            bar[player.ordinal()] = homeOnly || random.nextInt(4) != 0 ? 0 : 1 + random.nextInt(2);
            int toPlace = Board.CHECKERS_PER_PLAYER - off[player.ordinal()] - bar[player.ordinal()];
            for (int c = 0; c < toPlace; c++) {
                List<Integer> candidates = new ArrayList<>();
                for (int pip = 1; pip <= (homeOnly ? 6 : Board.POINTS); pip++) {
                    int point = Board.point(player, pip);
                    if (points[point - 1] * sign >= 0) {
                        candidates.add(point);
                    }
                }
                if (candidates.isEmpty()) {
                    off[player.ordinal()]++;
                } else {
                    int point = candidates.get(random.nextInt(candidates.size()));
                    points[point - 1] += sign;
                }
            }
        }
        List<Integer> list = new ArrayList<>();
        for (int value : points) {
            list.add(value);
        }
        return new Board(list, bar[0], bar[1], off[0], off[1]);
    }
}
