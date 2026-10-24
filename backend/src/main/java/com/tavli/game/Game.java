package com.tavli.game;

import com.tavli.engine.GameState;
import java.util.UUID;

/** A stored game: its engine state and the number of the turn now being played (1 for the opening turn). */
public record Game(UUID id, int turnNumber, GameState state) {
}
