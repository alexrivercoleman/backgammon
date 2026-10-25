package com.tavli.engine;

/** One round of the opening: each player's single die. */
public record OpeningThrow(int white, int black) {

    public OpeningThrow {
        Roll.requireDie(white);
        Roll.requireDie(black);
    }

    public boolean isTie() {
        return white == black;
    }
}
