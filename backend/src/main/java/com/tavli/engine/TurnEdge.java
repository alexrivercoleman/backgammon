package com.tavli.engine;

/** A move from one {@link TurnNode} to the node with id {@code next}; {@code hit} if it sends a blot to the bar. */
public record TurnEdge(Move move, boolean hit, int next) {
}
