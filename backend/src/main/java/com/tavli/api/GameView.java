package com.tavli.api;

import com.tavli.engine.Board;
import com.tavli.engine.GameState;
import com.tavli.engine.OpeningThrow;
import com.tavli.engine.Phase;
import com.tavli.engine.Player;
import com.tavli.engine.TurnEdge;
import com.tavli.engine.TurnNode;
import com.tavli.engine.TurnPlan;
import com.tavli.game.Game;
import com.tavli.game.MoveJson;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A game as sent to the client: the current position, what the game is waiting for, and, while a player is
 * moving, every legal way to play their roll as a graph the client can step through without knowing the rules.
 *
 * @param phase         OPENING (each player throws one die), ROLLING, MOVING or FINISHED
 * @param toMove        who acts next: in the OPENING, whose opening die is to be thrown
 * @param dice          the roll to play; only while MOVING
 * @param openingDie    WHITE's opening die while BLACK has yet to throw theirs; otherwise null
 * @param openingThrows the opening rounds so far as {@code [white, black]}, ties included; empty after turn 1
 * @param turn          the turn graph; only while MOVING
 */
public record GameView(UUID id, String status, Phase phase, int turnNumber, Player toMove, List<Integer> dice,
                       Integer openingDie, List<List<Integer>> openingThrows, Player winner, BoardView board,
                       TurnView turn) {

    /**
     * @param points signed checker counts for points 1..24 (index 0 is point 1): positive WHITE, negative BLACK
     */
    public record BoardView(List<Integer> points, Map<Player, Integer> bar, Map<Player, Integer> off) {

        static BoardView of(Board board) {
            return new BoardView(board.points(),
                    Map.of(Player.WHITE, board.whiteBar(), Player.BLACK, board.blackBar()),
                    Map.of(Player.WHITE, board.whiteOff(), Player.BLACK, board.blackOff()));
        }
    }

    /** Node 0 is the position before any move; the turn may be submitted once a {@code finished} node is reached. */
    public record TurnView(boolean mustPass, List<NodeView> nodes) {

        static TurnView of(TurnPlan plan) {
            return new TurnView(plan.mustPass(), plan.nodes().stream().map(NodeView::of).toList());
        }
    }

    public record NodeView(int id, BoardView board, List<Integer> unusedDice, boolean finished, List<EdgeView> moves) {

        static NodeView of(TurnNode node) {
            return new NodeView(node.id(), BoardView.of(node.board()), node.unusedDice(), node.finished(),
                    node.edges().stream().map(EdgeView::of).toList());
        }
    }

    /** A legal move from the enclosing node, leading to node {@code next}. */
    public record EdgeView(Object from, Object to, int die, boolean hit, int next) {

        static EdgeView of(TurnEdge edge) {
            MoveJson move = MoveJson.of(edge.move());
            return new EdgeView(move.from(), move.to(), move.die(), edge.hit(), edge.next());
        }
    }

    static GameView of(Game game, TurnPlan plan, List<OpeningThrow> openingThrows) {
        GameState state = game.state();
        return new GameView(
                game.id(),
                state.isOver() ? "FINISHED" : "IN_PROGRESS",
                state.phase(),
                game.turnNumber(),
                state.toMove(),
                state.roll() == null ? null : List.of(state.roll().first(), state.roll().second()),
                state.openingDie(),
                openingThrows.stream().map(t -> List.of(t.white(), t.black())).toList(),
                state.winner(),
                BoardView.of(state.board()),
                plan == null ? null : TurnView.of(plan));
    }
}
