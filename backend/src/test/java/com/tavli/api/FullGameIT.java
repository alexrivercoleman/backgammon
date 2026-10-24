package com.tavli.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.tavli.IntegrationTestConfiguration;
import com.tavli.ScriptedDice;
import com.tavli.engine.Dice;
import com.tavli.engine.GameState;
import com.tavli.engine.Roll;
import com.tavli.engine.TavliEngine;
import com.tavli.game.LogEntry;
import com.tavli.game.LogEntry.Kind;
import com.tavli.game.GameService;
import com.tavli.game.persistence.GameRepository;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Plays whole games through the HTTP API, choosing a random path through each turn's graph, then checks the
 * move log alone is enough to replay the game to exactly the stored final state.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationTestConfiguration.class)
class FullGameIT {

    private static final int MAX_TURNS = 2_000;

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JsonMapper mapper;

    @Autowired
    ScriptedDice dice;

    @Autowired
    GameService service;

    @Autowired
    GameRepository games;

    @ParameterizedTest
    @ValueSource(longs = {1, 2, 3})
    void gamesPlayedThroughTheApiFinishAndReplayFromTheLog(long seed) {
        dice.reset();
        dice.thenSeeded(seed);
        Random choices = new Random(seed);

        JsonNode game = json(mvc.post().uri("/api/games").exchange());
        String id = game.get("id").asString();
        int turns = 0;
        while (game.get("status").asString().equals("IN_PROGRESS")) {
            assertThat(++turns).as("game with seed %d is taking too long", seed).isLessThan(MAX_TURNS);
            ArrayNode moves = randomPath(game.get("turn").get("nodes"), choices);
            MvcTestResult result = mvc.post().uri("/api/games/{id}/turns", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.createObjectNode()
                            .put("turnNumber", game.get("turnNumber").asInt())
                            .set("moves", moves)
                            .toString())
                    .exchange();
            assertThat(result).hasStatusOk();
            game = json(result);
        }

        assertThat(game.get("winner").isNull()).isFalse();
        assertThat(game.get("board").get("off").get(game.get("winner").asString()).asInt()).isEqualTo(15);

        UUID gameId = UUID.fromString(id);
        GameState replayed = replay(service.log(gameId));
        assertThat(replayed).isEqualTo(games.find(gameId).orElseThrow().state());
    }

    /** Walks from node 0 to a finished node, picking a random legal move at each step. */
    private ArrayNode randomPath(JsonNode nodes, Random random) {
        ArrayNode path = mapper.createArrayNode();
        JsonNode node = nodes.get(0);
        while (!node.get("finished").asBoolean()) {
            JsonNode edge = node.get("moves").get(random.nextInt(node.get("moves").size()));
            ObjectNode move = path.addObject();
            move.set("from", edge.get("from"));
            move.set("to", edge.get("to"));
            move.set("die", edge.get("die"));
            node = nodes.get(edge.get("next").asInt());
        }
        return path;
    }

    /**
     * Rebuilds the game from its log: the opening throws and rolls supply the dice, and each turn is replayed
     * through a fresh engine. Also checks the log's shape: rolls and turns alternate with increasing turn
     * numbers and alternating players.
     */
    private static GameState replay(List<LogEntry> log) {
        assertThat(log).extracting(LogEntry::seq)
                .containsExactlyElementsOf(IntStream.rangeClosed(1, log.size()).boxed().toList());

        Deque<Integer> diceValues = new ArrayDeque<>();
        Iterator<LogEntry> entries = log.iterator();
        LogEntry entry = entries.next();
        while (entry.kind() == Kind.OPENING_THROW) {
            diceValues.addAll(entry.dice());
            entry = entries.next();
        }
        Dice logged = diceValues::pop;
        TavliEngine engine = new TavliEngine(logged);
        GameState state = engine.start().state();

        int expectedTurn = 1;
        while (true) {
            assertThat(entry.kind()).isEqualTo(Kind.ROLL);
            assertThat(entry.turnNumber()).isEqualTo(expectedTurn);
            assertThat(entry.player()).isEqualTo(state.toMove());
            assertThat(state.roll()).isEqualTo(new Roll(entry.dice().get(0), entry.dice().get(1)));

            LogEntry turn = entries.next();
            assertThat(turn.kind()).isEqualTo(Kind.TURN);
            assertThat(turn.turnNumber()).isEqualTo(expectedTurn);
            assertThat(turn.player()).isEqualTo(state.toMove());
            if (entries.hasNext()) {
                entry = entries.next();
                diceValues.addAll(entry.dice());
            }
            state = engine.play(state, turn.moves());
            expectedTurn++;
            if (state.isOver()) {
                assertThat(entries.hasNext()).isFalse();
                return state;
            }
        }
    }

    private JsonNode json(MvcTestResult result) {
        try {
            return mapper.readTree(result.getResponse().getContentAsString());
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
