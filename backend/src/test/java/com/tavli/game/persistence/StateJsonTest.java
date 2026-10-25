package com.tavli.game.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tavli.engine.Board;
import com.tavli.engine.GameState;
import com.tavli.engine.Move;
import com.tavli.engine.Phase;
import com.tavli.engine.Player;
import com.tavli.engine.Roll;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

class StateJsonTest {

    private final StateJson json = new StateJson(JsonMapper.builder().build());

    @Test
    void startingStateRoundTrips() {
        GameState state = GameState.moving(Board.starting(), Player.BLACK, new Roll(3, 6));

        String stored = json.write(state);

        assertThat(json.read(stored)).isEqualTo(state);
        assertThat(stored).contains("\"v\":2").contains("[-2,0,0,0,0,5,0,3,0,0,0,-5,5,0,0,0,-3,0,-5,0,0,0,0,2]");
    }

    @Test
    void barAndBorneOffCheckersRoundTrip() {
        GameState state = GameState.moving(board(List.of(6, 5, -19, -4), 1, 2, 7, 9), Player.WHITE, new Roll(2, 2));

        assertThat(json.read(json.write(state))).isEqualTo(state);
    }

    @Test
    void finishedStateRoundTrips() {
        GameState state = GameState.finished(board(List.of(-24, -3), 0, 0, 15, 12), Player.WHITE);

        GameState read = json.read(json.write(state));

        assertThat(read).isEqualTo(state);
        assertThat(read.isOver()).isTrue();
    }

    @Test
    void openingAndRollingPhasesRoundTrip() {
        GameState whiteThrew = new GameState(Phase.OPENING, Board.starting(), Player.BLACK, null, 4, null);
        GameState rolling = GameState.awaitingRoll(Board.starting(), Player.WHITE);

        assertThat(json.read(json.write(GameState.opening()))).isEqualTo(GameState.opening());
        assertThat(json.read(json.write(whiteThrew))).isEqualTo(whiteThrew);
        assertThat(json.read(json.write(rolling))).isEqualTo(rolling);
    }

    @Test
    void readsVersionOneStatesAsWaitingForAMove() {
        String v1 = "{\"v\":1,\"board\":{\"points\":[-2,0,0,0,0,5,0,3,0,0,0,-5,5,0,0,0,-3,0,-5,0,0,0,0,2],"
                + "\"bar\":{\"WHITE\":0,\"BLACK\":0},\"off\":{\"WHITE\":0,\"BLACK\":0}},"
                + "\"toMove\":\"BLACK\",\"roll\":[3,6],\"winner\":null}";

        assertThat(json.read(v1)).isEqualTo(GameState.moving(Board.starting(), Player.BLACK, new Roll(3, 6)));
    }

    @Test
    void rejectsUnknownVersionsAndInvalidBoards() {
        String valid = json.write(GameState.moving(Board.starting(), Player.WHITE, new Roll(1, 2)));

        assertThatThrownBy(() -> json.read(valid.replace("\"v\":2", "\"v\":9")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("version");
        assertThatThrownBy(() -> json.read(valid.replace("[-2,", "[-3,")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid");
    }

    @Test
    void movesUseBarAndOffKeywords() {
        List<Move> moves = List.of(new Move(Move.BAR, 20, 5), new Move(13, 7, 6), new Move(3, Move.OFF, 4));

        String stored = json.writeMoves(moves);

        assertThat(stored).contains("\"from\":\"bar\"").contains("\"to\":\"off\"").contains("\"from\":13");
        assertThat(json.readMoves(stored)).isEqualTo(moves);
        assertThat(json.readMoves("[]")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"from\":0,\"to\":5,\"die\":1}",
            "{\"from\":25,\"to\":20,\"die\":5}",
            "{\"from\":\"foo\",\"to\":20,\"die\":5}",
            "{\"from\":\"off\",\"to\":20,\"die\":5}",
            "{\"from\":13,\"to\":\"bar\",\"die\":5}",
            "{\"from\":13.5,\"to\":8,\"die\":5}",
            "{\"from\":13,\"to\":8,\"die\":7}",
    })
    void rejectsInvalidMoves(String move) {
        assertThatThrownBy(() -> json.readMoves("[" + move + "]")).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Builds a board from signed point placements: {@code 6} is one WHITE checker on point 6, {@code -19} one
     * BLACK checker on point 19.
     */
    private static Board board(List<Integer> placements, int whiteBar, int blackBar, int whiteOff, int blackOff) {
        List<Integer> points = new ArrayList<>(Collections.nCopies(24, 0));
        for (int placement : placements) {
            int index = Math.abs(placement) - 1;
            points.set(index, points.get(index) + Integer.signum(placement));
        }
        int white = 15 - whiteBar - whiteOff - (int) placements.stream().filter(p -> p > 0).count();
        int black = 15 - blackBar - blackOff - (int) placements.stream().filter(p -> p < 0).count();
        // Park any remaining checkers on each side's own 1-point so the board is complete.
        points.set(0, points.get(0) + white);
        points.set(23, points.get(23) - black);
        return new Board(points, whiteBar, blackBar, whiteOff, blackOff);
    }
}
