package com.tavli.game;

import com.tavli.engine.Player;
import java.util.UUID;

public class GameFinishedException extends RuntimeException {

    public GameFinishedException(UUID id, Player winner) {
        super("Game " + id + " is over; " + winner + " won");
    }
}
