package com.tavli.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Immutable checker layout.
 *
 * @param points signed checker counts for absolute points 1..24 (index 0 is point 1): positive for WHITE,
 *               negative for BLACK
 */
public record Board(List<Integer> points, int whiteBar, int blackBar, int whiteOff, int blackOff) {

    public static final int CHECKERS_PER_PLAYER = 15;
    public static final int POINTS = 24;

    public Board {
        points = List.copyOf(points);
        if (points.size() != POINTS) {
            throw new IllegalArgumentException("Board must have 24 points, had " + points.size());
        }
        if (whiteBar < 0 || blackBar < 0 || whiteOff < 0 || blackOff < 0) {
            throw new IllegalArgumentException("Bar and off counts must be non-negative");
        }
        int white = whiteBar + whiteOff;
        int black = blackBar + blackOff;
        for (int value : points) {
            if (value > 0) {
                white += value;
            } else {
                black -= value;
            }
        }
        if (white != CHECKERS_PER_PLAYER || black != CHECKERS_PER_PLAYER) {
            throw new IllegalArgumentException(
                    "Each player needs 15 checkers, had white=" + white + " black=" + black);
        }
    }

    /** Standard backgammon starting position. */
    public static Board starting() {
        Integer[] p = new Integer[POINTS];
        Arrays.fill(p, 0);
        p[24 - 1] = 2;
        p[13 - 1] = 5;
        p[8 - 1] = 3;
        p[6 - 1] = 5;
        p[1 - 1] = -2;
        p[12 - 1] = -5;
        p[17 - 1] = -3;
        p[19 - 1] = -5;
        return new Board(List.of(p), 0, 0, 0, 0);
    }

    /**
     * Converts a pip (the mover's distance from bearing off, 1..24) to an absolute point. The mapping is its
     * own inverse, so it also converts points to pips.
     */
    public static int point(Player player, int pip) {
        return player == Player.WHITE ? pip : POINTS + 1 - pip;
    }

    /** Number of {@code player}'s checkers on absolute {@code point}. */
    public int count(Player player, int point) {
        int value = points.get(point - 1);
        return Math.max(0, player == Player.WHITE ? value : -value);
    }

    public int bar(Player player) {
        return player == Player.WHITE ? whiteBar : blackBar;
    }

    public int off(Player player) {
        return player == Player.WHITE ? whiteOff : blackOff;
    }

    /** True when the player has no checkers on the bar or outside their home board. */
    public boolean allHome(Player player) {
        if (bar(player) > 0) {
            return false;
        }
        for (int pip = 7; pip <= POINTS; pip++) {
            if (count(player, point(player, pip)) > 0) {
                return false;
            }
        }
        return true;
    }

    /** Highest pip the player occupies on the board, or 0 if none. */
    public int highestPip(Player player) {
        for (int pip = POINTS; pip >= 1; pip--) {
            if (count(player, point(player, pip)) > 0) {
                return pip;
            }
        }
        return 0;
    }

    /**
     * Returns the board after {@code player} makes {@code move}, hitting a lone opposing checker on the
     * destination. Only basic consistency is checked here; rule legality is the generator's job.
     */
    Board apply(Player player, Move move) {
        List<Integer> next = new ArrayList<>(points);
        int sign = player == Player.WHITE ? 1 : -1;
        int[] bar = {whiteBar, blackBar};
        int[] off = {whiteOff, blackOff};
        int me = player.ordinal();
        int them = player.opponent().ordinal();

        if (move.isEntry()) {
            if (bar[me] == 0) {
                throw new IllegalStateException("No checker on the bar for " + move);
            }
            bar[me]--;
        } else {
            if (count(player, move.from()) == 0) {
                throw new IllegalStateException("No checker on point " + move.from() + " for " + move);
            }
            next.set(move.from() - 1, next.get(move.from() - 1) - sign);
        }

        if (move.isBearOff()) {
            off[me]++;
        } else {
            int opposing = count(player.opponent(), move.to());
            if (opposing > 1) {
                throw new IllegalStateException("Point " + move.to() + " is blocked for " + move);
            }
            if (opposing == 1) {
                next.set(move.to() - 1, 0);
                bar[them]++;
            }
            next.set(move.to() - 1, next.get(move.to() - 1) + sign);
        }
        return new Board(next, bar[0], bar[1], off[0], off[1]);
    }

    @Override
    public String toString() {
        return describe(Player.WHITE) + " | " + describe(Player.BLACK);
    }

    private String describe(Player player) {
        StringBuilder sb = new StringBuilder(player == Player.WHITE ? "W:" : "B:");
        for (int point = 1; point <= POINTS; point++) {
            int n = count(player, point);
            if (n > 0) {
                sb.append(' ').append(point).append('x').append(n);
            }
        }
        return sb.append(" bar=").append(bar(player)).append(" off=").append(off(player)).toString();
    }
}
