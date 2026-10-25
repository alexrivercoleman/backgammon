package com.tavli.api;

import com.tavli.engine.Move;
import com.tavli.game.Game;
import com.tavli.game.GameService;
import com.tavli.game.MoveJson;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService games;

    public GameController(GameService games) {
        this.games = games;
    }

    @PostMapping
    public ResponseEntity<GameView> create() {
        Game game = games.create();
        return ResponseEntity.created(URI.create("/api/games/" + game.id())).body(view(game));
    }

    @GetMapping("/{id}")
    public GameView get(@PathVariable UUID id) {
        return view(games.get(id));
    }

    @PostMapping("/{id}/turns")
    public GameView submitTurn(@PathVariable UUID id, @RequestBody SubmitTurnRequest request) {
        if (request.turnNumber() == null || request.moves() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "turnNumber and moves are required");
        }
        return view(games.play(id, request.turnNumber(), toMoves(request.moves())));
    }

    @PostMapping("/{id}/roll")
    public GameView roll(@PathVariable UUID id, @RequestBody RollRequest request) {
        if (request.turnNumber() == null || request.player() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "turnNumber and player are required");
        }
        return view(games.roll(id, request.turnNumber(), request.player()));
    }

    @GetMapping("/{id}/log")
    public List<LogEntryView> log(@PathVariable UUID id) {
        return games.log(id).stream().map(LogEntryView::of).toList();
    }

    private GameView view(Game game) {
        return GameView.of(game, games.currentTurn(game).orElse(null), games.openingThrows(game));
    }

    private static List<Move> toMoves(List<MoveJson> moves) {
        try {
            return moves.stream().map(MoveJson::toMove).toList();
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid move: " + e.getMessage(), e);
        }
    }
}
