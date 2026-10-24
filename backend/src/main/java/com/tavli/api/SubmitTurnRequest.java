package com.tavli.api;

import com.tavli.game.MoveJson;
import java.util.List;

/**
 * @param turnNumber the turn being played, as last received in {@link GameView#turnNumber()}
 * @param moves      the complete turn in the order played; empty to pass
 */
public record SubmitTurnRequest(Integer turnNumber, List<MoveJson> moves) {
}
