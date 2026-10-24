package com.tavli.api;

import com.tavli.engine.Board;
import com.tavli.engine.GameState;
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
 * A game as sent to the client: the current position plus, while the game is in progress, every legal way to
 * play the current roll as a graph the client can step through without knowing the rules.
 *
 * @param turn null once the game is finished
 */
public record GameView(UUID id, String status, int turnNumber, Player toMove, List<Integer> dice, Player winner,
                       BoardView board, TurnView turn) {

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

    static GameView of(Game game, TurnPlan plan) {
        GameState state = game.state();
        return new GameView(
                game.id(),
                state.isOver() ? "FINISHED" : "IN_PROGRESS",
                game.turnNumber(),
                state.toMove(),
                state.roll() == null ? null : List.of(state.roll().first(), state.roll().second()),
                state.winner(),
                BoardView.of(state.board()),
                plan == null ? null : TurnView.of(plan));
    }
}
