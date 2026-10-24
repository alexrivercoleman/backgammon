package com.tavli.engine;

/**
 * One checker moved by one die. Points are absolute (1..24); {@link #BAR} as the origin means entering from
 * the bar and {@link #OFF} as the destination means bearing off.
 */
public record Move(int from, int to, int die) {

    public static final int BAR = -1;
    public static final int OFF = -2;

    public Move {
        if (from != BAR && (from < 1 || from > 24)) {
            throw new IllegalArgumentException("Invalid origin: " + from);
        }
        if (to != OFF && (to < 1 || to > 24)) {
            throw new IllegalArgumentException("Invalid destination: " + to);
        }
        Roll.requireDie(die);
    }

    public boolean isEntry() {
        return from == BAR;
    }

    public boolean isBearOff() {
        return to == OFF;
    }

    @Override
    public String toString() {
        return (isEntry() ? "bar" : String.valueOf(from)) + "/" + (isBearOff() ? "off" : String.valueOf(to))
                + "(" + die + ")";
    }
}
