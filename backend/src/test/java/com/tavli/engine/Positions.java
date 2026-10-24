package com.tavli.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Test helpers for building positions and moves. Positions and move notation are written from WHITE's point
 * of view; {@link #forPlayer} mirrors them so the same scenario can be checked for BLACK.
 */
final class Positions {

    private Positions() {
    }

    static Builder board() {
        return new Builder();
    }

    /** Swaps colours and reflects the board, so a WHITE scenario becomes the same scenario for BLACK. */
    static Board mirror(Board board) {
        List<Integer> points = new ArrayList<>(Board.POINTS);
        for (int point = 1; point <= Board.POINTS; point++) {
            points.add(-board.points().get(Board.POINTS - point));
        }
        return new Board(points, board.blackBar(), board.whiteBar(), board.blackOff(), board.whiteOff());
    }

    static Move mirror(Move move) {
        int from = move.isEntry() ? Move.BAR : Board.POINTS + 1 - move.from();
        int to = move.isBearOff() ? Move.OFF : Board.POINTS + 1 - move.to();
        return new Move(from, to, move.die());
    }

    static Board forPlayer(Player player, Board whiteView) {
        return player == Player.WHITE ? whiteView : mirror(whiteView);
    }

    static Move forPlayer(Player player, Move whiteView) {
        return player == Player.WHITE ? whiteView : mirror(whiteView);
    }

    static List<Move> forPlayer(Player player, List<Move> whiteView) {
        return whiteView.stream().map(m -> forPlayer(player, m)).toList();
    }

    /**
     * Parses WHITE-perspective notation such as {@code "bar/22 13/10 6/off 2/off:6"}. The die is the distance
     * moved unless given explicitly after a colon (needed when bearing off with a higher die).
     */
    static List<Move> moves(String notation) {
        if (notation.isBlank()) {
            return List.of();
        }
        return Arrays.stream(notation.trim().split("\\s+")).map(Positions::move).toList();
    }

    static List<Move> moves(Player player, String notation) {
        return forPlayer(player, moves(notation));
    }

    static Move move(String token) {
        String[] dieSplit = token.split(":");
        String[] parts = dieSplit[0].split("/");
        int fromPip = parts[0].equals("bar") ? 25 : Integer.parseInt(parts[0]);
        int toPip = parts[1].equals("off") ? 0 : Integer.parseInt(parts[1]);
        int die = dieSplit.length > 1 ? Integer.parseInt(dieSplit[1]) : fromPip - toPip;
        return new Move(fromPip == 25 ? Move.BAR : fromPip, toPip == 0 ? Move.OFF : toPip, die);
    }

    /** Builds a board from WHITE-perspective points; any checkers not placed count as borne off. */
    static final class Builder {
        private final int[] points = new int[Board.POINTS];
        private int whiteBar;
        private int blackBar;

        Builder white(int point, int count) {
            points[point - 1] += count;
            return this;
        }

        Builder black(int point, int count) {
            points[point - 1] -= count;
            return this;
        }

        Builder whiteBar(int count) {
            whiteBar += count;
            return this;
        }

        Builder blackBar(int count) {
            blackBar += count;
            return this;
        }

        Board build() {
            int white = whiteBar;
            int black = blackBar;
            for (int value : points) {
                white += Math.max(0, value);
                black += Math.max(0, -value);
            }
            return new Board(Arrays.stream(points).boxed().toList(), whiteBar, blackBar,
                    Board.CHECKERS_PER_PLAYER - white, Board.CHECKERS_PER_PLAYER - black);
        }
    }
}
