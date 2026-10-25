package com.tavli.game;

/** The game is not waiting for this action from this player, e.g. a move before the dice are rolled. */
public class ActionNotAllowedException extends RuntimeException {

    public ActionNotAllowedException(String message) {
        super(message);
    }
}
