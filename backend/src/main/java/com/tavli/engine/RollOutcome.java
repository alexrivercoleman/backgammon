package com.tavli.engine;

/**
 * The result of {@link TavliEngine#roll}.
 *
 * @param openingThrow the opening round completed by this roll (BLACK's opening die), or null
 */
public record RollOutcome(GameState state, OpeningThrow openingThrow) {
}
