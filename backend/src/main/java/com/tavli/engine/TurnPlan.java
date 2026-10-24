package com.tavli.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Every legal way to play one roll, as a graph of positions.
 *
 * <p>Nodes are (board, dice still unused); edges are single checker moves. Different move orders that reach
 * the same position share a node, so equivalent sequences are deduplicated without rejecting any order a
 * player might choose. A node <em>finishes</em> the turn when it uses as many dice as any sequence can (with
 * the higher die preferred when only one die can be played) or when it bears off the player's last checker.
 * Edges that cannot reach a finishing node are pruned, so every move offered by {@link #nextMoves} can be
 * completed into a legal turn.
 */
public final class TurnPlan {

    private record Node(Board board, List<Integer> unused) {
    }

    private final Player player;
    private final Roll roll;
    private final Node root;
    private final Map<Node, Map<Move, Node>> edges;
    private final Set<Node> finishing;

    private TurnPlan(Player player, Roll roll, Node root, Map<Node, Map<Move, Node>> edges, Set<Node> finishing) {
        this.player = player;
        this.roll = roll;
        this.root = root;
        this.edges = edges;
        this.finishing = finishing;
    }

    public static TurnPlan of(Board board, Player player, Roll roll) {
        Node root = new Node(board, roll.dice());
        Map<Node, Map<Move, Node>> all = new HashMap<>();
        explore(root, player, all);

        int diceTotal = roll.dice().size();
        int maxUsed = all.keySet().stream().mapToInt(n -> diceTotal - n.unused().size()).max().orElse(0);

        Set<Node> deepest = new HashSet<>();
        Set<Node> finishing = new HashSet<>();
        for (Node node : all.keySet()) {
            if (node.board().off(player) == Board.CHECKERS_PER_PLAYER) {
                finishing.add(node);
            } else if (diceTotal - node.unused().size() == maxUsed) {
                deepest.add(node);
            }
        }
        if (maxUsed == 1 && !roll.isDouble()) {
            Set<Node> usedHigher = new HashSet<>();
            for (Node node : deepest) {
                if (!node.unused().contains(roll.higher())) {
                    usedHigher.add(node);
                }
            }
            if (!usedHigher.isEmpty()) {
                deepest = usedHigher;
            }
        }
        finishing.addAll(deepest);

        return new TurnPlan(player, roll, root, prune(root, all, finishing), Set.copyOf(finishing));
    }

    private static void explore(Node node, Player player, Map<Node, Map<Move, Node>> all) {
        if (all.containsKey(node)) {
            return;
        }
        Map<Move, Node> out = new LinkedHashMap<>();
        all.put(node, out);
        for (int die : new LinkedHashSet<>(node.unused())) {
            List<Integer> rest = new ArrayList<>(node.unused());
            rest.remove(Integer.valueOf(die));
            for (Move move : MoveGenerator.singleMoves(node.board(), player, die)) {
                out.put(move, new Node(node.board().apply(player, move), List.copyOf(rest)));
            }
        }
        for (Node child : out.values()) {
            explore(child, player, all);
        }
    }

    /** Keeps only the nodes and edges that lie on a path from the root to a finishing node. */
    private static Map<Node, Map<Move, Node>> prune(Node root, Map<Node, Map<Move, Node>> all, Set<Node> finishing) {
        Map<Node, Boolean> reachesFinish = new HashMap<>();
        reaches(root, all, finishing, reachesFinish);

        Map<Node, Map<Move, Node>> pruned = new HashMap<>();
        for (Map.Entry<Node, Map<Move, Node>> entry : all.entrySet()) {
            if (!reachesFinish.getOrDefault(entry.getKey(), false)) {
                continue;
            }
            Map<Move, Node> kept = new LinkedHashMap<>();
            entry.getValue().forEach((move, child) -> {
                if (reachesFinish.get(child)) {
                    kept.put(move, child);
                }
            });
            pruned.put(entry.getKey(), Collections.unmodifiableMap(kept));
        }
        return Collections.unmodifiableMap(pruned);
    }

    private static boolean reaches(Node node, Map<Node, Map<Move, Node>> all, Set<Node> finishing,
                                   Map<Node, Boolean> memo) {
        Boolean known = memo.get(node);
        if (known != null) {
            return known;
        }
        boolean result = finishing.contains(node);
        for (Node child : all.get(node).values()) {
            // Evaluate every child so the memo covers the whole graph.
            result |= reaches(child, all, finishing, memo);
        }
        memo.put(node, result);
        return result;
    }

    public Player player() {
        return player;
    }

    public Roll roll() {
        return roll;
    }

    public Board start() {
        return root.board();
    }

    /** True when no move can be made, so the only legal turn is the empty one. */
    public boolean mustPass() {
        return finishing.contains(root);
    }

    /**
     * Moves that may follow {@code prefix} and still lead to a legal turn; empty once the turn is complete.
     *
     * @throws IllegalArgumentException if {@code prefix} is not the start of a legal turn
     */
    public List<Move> nextMoves(List<Move> prefix) {
        return List.copyOf(edges.get(walk(prefix)).keySet());
    }

    public boolean isLegal(List<Move> turn) {
        Node end = tryWalk(turn);
        return end != null && finishing.contains(end);
    }

    /**
     * Board after playing {@code turn}.
     *
     * @throws IllegalArgumentException if {@code turn} is not a complete legal turn
     */
    public Board resultOf(List<Move> turn) {
        Node end = walk(turn);
        if (!finishing.contains(end)) {
            throw new IllegalArgumentException("Turn is incomplete: " + turn);
        }
        return end.board();
    }

    /** Every legal turn, including each ordering of the same moves. */
    public Set<List<Move>> sequences() {
        Set<List<Move>> result = new LinkedHashSet<>();
        collect(root, new ArrayList<>(), result);
        return Collections.unmodifiableSet(result);
    }

    /** The distinct positions a legal turn can end in. */
    public Set<Board> outcomes() {
        Set<Board> result = new LinkedHashSet<>();
        for (Node node : finishing) {
            result.add(node.board());
        }
        return Collections.unmodifiableSet(result);
    }

    private void collect(Node node, List<Move> path, Set<List<Move>> out) {
        if (finishing.contains(node)) {
            out.add(List.copyOf(path));
        }
        for (Map.Entry<Move, Node> edge : edges.get(node).entrySet()) {
            path.add(edge.getKey());
            collect(edge.getValue(), path, out);
            path.removeLast();
        }
    }

    private Node walk(List<Move> moves) {
        Node node = tryWalk(moves);
        if (node == null) {
            throw new IllegalArgumentException("Not a legal sequence for " + player + " with " + roll + ": " + moves);
        }
        return node;
    }

    private Node tryWalk(List<Move> moves) {
        Node node = root;
        for (Move move : moves) {
            Map<Move, Node> out = edges.get(node);
            node = out == null ? null : out.get(move);
            if (node == null) {
                return null;
            }
        }
        return node;
    }
}
