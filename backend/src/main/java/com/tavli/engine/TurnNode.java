package com.tavli.engine;

import java.util.List;

/**
 * One position within a turn: the board so far, the dice not yet used, whether the turn may be confirmed here,
 * and the moves that continue towards a legal turn.
 */
public record TurnNode(int id, Board board, List<Integer> unusedDice, boolean finished, List<TurnEdge> edges) {

    public TurnNode {
        unusedDice = List.copyOf(unusedDice);
        edges = List.copyOf(edges);
    }
}
