package com.tavli.engine;

import java.util.ArrayList;
import java.util.List;

/** Generates the legal single-checker moves for one die, working in the mover's pips. */
final class MoveGenerator {

    private static final int BAR_PIP = 25;

    private MoveGenerator() {
    }

    static List<Move> singleMoves(Board board, Player player, int die) {
        List<Move> moves = new ArrayList<>();
        if (board.bar(player) > 0) {
            int target = Board.point(player, BAR_PIP - die);
            if (isOpen(board, player, target)) {
                moves.add(new Move(Move.BAR, target, die));
            }
            return moves;
        }
        for (int pip = Board.POINTS; pip >= 1; pip--) {
            int from = Board.point(player, pip);
            if (board.count(player, from) == 0) {
                continue;
            }
            int targetPip = pip - die;
            if (targetPip >= 1) {
                int to = Board.point(player, targetPip);
                if (isOpen(board, player, to)) {
                    moves.add(new Move(from, to, die));
                }
            }
        }
        return moves;
    }

    /** A point is open unless the opponent holds it with two or more checkers. */
    private static boolean isOpen(Board board, Player player, int point) {
        return board.count(player.opponent(), point) < 2;
    }
}
