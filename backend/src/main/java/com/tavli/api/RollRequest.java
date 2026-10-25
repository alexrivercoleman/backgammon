package com.tavli.api;

import com.tavli.engine.Player;

/**
 * @param turnNumber the turn being played, as last received in {@link GameView#turnNumber()}
 * @param player     who is rolling; must be {@link GameView#toMove()}, so a repeated click cannot roll for the
 *                   other player
 */
public record RollRequest(Integer turnNumber, Player player) {
}
