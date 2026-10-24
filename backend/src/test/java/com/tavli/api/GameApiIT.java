package com.tavli.api;

import static com.tavli.engine.Positions.board;
import static org.assertj.core.api.Assertions.assertThat;

import com.tavli.IntegrationTestConfiguration;
import com.tavli.ScriptedDice;
import com.tavli.engine.Board;
import com.tavli.engine.GameState;
import com.tavli.engine.Player;
import com.tavli.engine.Roll;
import com.tavli.game.Game;
import com.tavli.game.persistence.GameRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationTestConfiguration.class)
class GameApiIT {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JsonMapper mapper;

    @Autowired
    ScriptedDice dice;

    @Autowired
    GameRepository games;

    @BeforeEach
    void resetDice() {
        dice.reset();
    }

    @Test
    void createReturnsTheNewGameWithItsTurnGraph() {
        dice.script(3, 6);

        MvcTestResult result = mvc.post().uri("/api/games").exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        JsonNode game = json(result);
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("/api/games/" + game.get("id").asString());
        assertThat(game.get("status").asString()).isEqualTo("IN_PROGRESS");
        assertThat(game.get("turnNumber").asInt()).isEqualTo(1);
        assertThat(game.get("toMove").asString()).isEqualTo("BLACK");
        assertThat(ints(game.get("dice"))).containsExactly(3, 6);
        assertThat(game.get("winner").isNull()).isTrue();
        assertThat(ints(game.get("board").get("points"))).isEqualTo(Board.starting().points());
        assertThat(game.get("board").get("bar").get("WHITE").asInt()).isZero();

        JsonNode turn = game.get("turn");
        assertThat(turn.get("mustPass").asBoolean()).isFalse();
        JsonNode root = turn.get("nodes").get(0);
        assertThat(root.get("id").asInt()).isZero();
        assertThat(ints(root.get("unusedDice"))).containsExactly(3, 6);
        assertThat(root.get("finished").asBoolean()).isFalse();
        JsonNode firstMove = root.get("moves").get(0);
        assertThat(firstMove.has("from") && firstMove.has("to") && firstMove.has("die")
                && firstMove.has("hit") && firstMove.has("next")).isTrue();
    }

    @Test
    void getReturnsTheStoredGame() {
        dice.script(3, 6);
        String id = json(mvc.post().uri("/api/games").exchange()).get("id").asString();

        MvcTestResult result = mvc.get().uri("/api/games/{id}", id).exchange();

        assertThat(result).hasStatusOk();
        assertThat(json(result).get("turnNumber").asInt()).isEqualTo(1);
    }

    @Test
    void unknownOrMalformedIdsAreRejected() {
        MvcTestResult missing = mvc.get().uri("/api/games/{id}", UUID.randomUUID()).exchange();
        assertThat(missing).hasStatus(HttpStatus.NOT_FOUND).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(json(missing).get("title").asString()).isEqualTo("Game not found");

        assertThat(mvc.get().uri("/api/games/not-a-uuid").exchange()).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void aLegalTurnAdvancesTheGame() {
        dice.script(3, 1, 5, 2);
        String id = createdId();

        MvcTestResult result = submit(id, 1, """
                [{"from": 8, "to": 5, "die": 3}, {"from": 6, "to": 5, "die": 1}]""");

        assertThat(result).hasStatusOk();
        JsonNode game = json(result);
        assertThat(game.get("turnNumber").asInt()).isEqualTo(2);
        assertThat(game.get("toMove").asString()).isEqualTo("BLACK");
        assertThat(ints(game.get("dice"))).containsExactly(5, 2);
        assertThat(game.get("board").get("points").get(4).asInt()).isEqualTo(2);
    }

    @Test
    void anIllegalTurnIsUnprocessableAndChangesNothing() {
        dice.script(6, 5);
        String id = createdId();

        MvcTestResult result = submit(id, 1, """
                [{"from": 13, "to": 7, "die": 6}]""");

        assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(json(result).get("title").asString()).isEqualTo("Illegal turn");
        assertThat(json(mvc.get().uri("/api/games/{id}", id).exchange()).get("turnNumber").asInt()).isEqualTo(1);
    }

    @Test
    void aStaleTurnNumberConflicts() {
        dice.script(3, 1, 5, 2);
        String id = createdId();
        String turn = """
                [{"from": 8, "to": 5, "die": 3}, {"from": 6, "to": 5, "die": 1}]""";
        submit(id, 1, turn);

        MvcTestResult result = submit(id, 1, turn);

        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(json(result).get("title").asString()).isEqualTo("Stale turn");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not json",
            "{\"moves\": []}",
            "{\"turnNumber\": 1}",
            "{\"turnNumber\": 1, \"moves\": [{\"from\": \"foo\", \"to\": 5, \"die\": 3}]}",
            "{\"turnNumber\": 1, \"moves\": [{\"from\": 0, \"to\": 5, \"die\": 3}]}",
            "{\"turnNumber\": 1, \"moves\": [{\"to\": 5, \"die\": 3}]}",
            "{\"turnNumber\": 1, \"moves\": [{\"from\": 8, \"to\": 5, \"die\": 9}]}",
    })
    void malformedRequestsAreBadRequests(String body) {
        dice.script(3, 1);
        String id = createdId();

        MvcTestResult result = mvc.post().uri("/api/games/{id}/turns", id)
                .contentType(MediaType.APPLICATION_JSON).content(body).exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void barEntryUsesTheBarKeyword() {
        Board board = board().whiteBar(1).white(3, 1).white(1, 13).black(13, 15).build();
        String id = saved(new GameState(board, Player.WHITE, new Roll(5, 3), null), 12);
        dice.script(4, 2);

        JsonNode root = json(mvc.get().uri("/api/games/{id}", id).exchange()).get("turn").get("nodes").get(0);
        assertThat(root.get("moves")).allSatisfy(m -> assertThat(m.get("from").asString()).isEqualTo("bar"));

        MvcTestResult result = submit(id, 12, """
                [{"from": "bar", "to": 20, "die": 5}, {"from": 20, "to": 17, "die": 3}]""");

        assertThat(result).hasStatusOk();
        assertThat(json(result).get("board").get("bar").get("WHITE").asInt()).isZero();
        JsonNode logged = json(mvc.get().uri("/api/games/{id}/log", id).exchange()).get(0).get("moves");
        assertThat(logged.get(0).get("from").asString()).isEqualTo("bar");
    }

    @Test
    void aBlockedPlayerPasses() {
        Board blocked = board().whiteBar(1).white(13, 5)
                .black(19, 2).black(20, 2).black(21, 2).black(22, 2).black(23, 2).black(24, 2).build();
        String id = saved(new GameState(blocked, Player.WHITE, new Roll(6, 5), null), 4);
        dice.script(2, 1);

        JsonNode turn = json(mvc.get().uri("/api/games/{id}", id).exchange()).get("turn");
        assertThat(turn.get("mustPass").asBoolean()).isTrue();
        assertThat(turn.get("nodes")).hasSize(1);
        assertThat(turn.get("nodes").get(0).get("finished").asBoolean()).isTrue();

        MvcTestResult result = submit(id, 4, "[]");

        assertThat(result).hasStatusOk();
        assertThat(json(result).get("toMove").asString()).isEqualTo("BLACK");
    }

    @Test
    void theWinningTurnFinishesTheGame() {
        Board nearlyDone = board().white(3, 1).white(1, 1).black(13, 15).build();
        String id = saved(new GameState(nearlyDone, Player.WHITE, new Roll(3, 1), null), 30);

        MvcTestResult result = submit(id, 30, """
                [{"from": 3, "to": "off", "die": 3}, {"from": 1, "to": "off", "die": 1}]""");

        assertThat(result).hasStatusOk();
        JsonNode game = json(result);
        assertThat(game.get("status").asString()).isEqualTo("FINISHED");
        assertThat(game.get("winner").asString()).isEqualTo("WHITE");
        assertThat(game.get("dice").isNull()).isTrue();
        assertThat(game.get("turn").isNull()).isTrue();
        JsonNode logged = json(mvc.get().uri("/api/games/{id}/log", id).exchange()).get(0).get("moves");
        assertThat(logged).allSatisfy(m -> assertThat(m.get("to").asString()).isEqualTo("off"));

        MvcTestResult again = submit(id, 31, "[]");
        assertThat(again).hasStatus(HttpStatus.CONFLICT);
        assertThat(json(again).get("title").asString()).isEqualTo("Game is over");
    }

    @Test
    void theLogListsOpeningThrowsRollsAndTurns() {
        dice.script(2, 2, 3, 1, 5, 2);
        String id = createdId();
        submit(id, 1, """
                [{"from": 8, "to": 5, "die": 3}, {"from": 6, "to": 5, "die": 1}]""");

        JsonNode log = json(mvc.get().uri("/api/games/{id}/log", id).exchange());

        List<String> kinds = new ArrayList<>();
        log.forEach(e -> kinds.add(e.get("kind").asString()));
        assertThat(kinds).containsExactly("OPENING_THROW", "OPENING_THROW", "ROLL", "TURN", "ROLL");
        assertThat(ints(log.get(0).get("dice"))).containsExactly(2, 2);
        assertThat(log.get(0).get("player").isNull()).isTrue();
        assertThat(log.get(3).get("player").asString()).isEqualTo("WHITE");
        assertThat(log.get(3).get("moves").get(0).get("from").asInt()).isEqualTo(8);
        assertThat(log.get(4).get("turnNumber").asInt()).isEqualTo(2);
        assertThat(mvc.get().uri("/api/games/{id}/log", UUID.randomUUID()).exchange())
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    private String createdId() {
        return json(mvc.post().uri("/api/games").exchange()).get("id").asString();
    }

    private String saved(GameState state, int turnNumber) {
        Game game = new Game(UUID.randomUUID(), turnNumber, state);
        games.insert(game);
        return game.id().toString();
    }

    private MvcTestResult submit(String id, int turnNumber, String movesJson) {
        return mvc.post().uri("/api/games/{id}/turns", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"turnNumber\": " + turnNumber + ", \"moves\": " + movesJson + "}")
                .exchange();
    }

    private JsonNode json(MvcTestResult result) {
        try {
            return mapper.readTree(result.getResponse().getContentAsString());
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<Integer> ints(JsonNode array) {
        List<Integer> values = new ArrayList<>();
        array.forEach(n -> values.add(n.asInt()));
        return values;
    }
}
