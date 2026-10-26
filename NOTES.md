# Notes

## Decisions and Trade-offs

**Choosing the game.** Backgammon offered the best mix of complexity and personal connection. I would have loved
to build chess, but I felt 4 hours was too tight for a full "working, well-built" game. I steered away from
checkers and Connect Four for the opposite reason: they wouldn't leave much room to show engine design.
Backgammon has a surprising amount of depth given its simple look, and I knew it would be a fun way to show off
my engineering skills. It didn't hurt that I've been playing it with family for years.

**Stack.** Java/Spring Boot, PostgreSQL and React + TypeScript.

- Java for familiarity, plus records and strong typing in the rules engine, accepting a heavier runtime than Go.
- Postgres for JSONB, transactional migrations and database-enforced constraints on the move log.
- React for familiarity. Since the client only draws the board and walks the server's move graph, the framework
  choice mattered less.

**Building the game.**

- I chose to hold all rules code in the backend, having the server roll the dice and validate move sequences,
  returning a list of all legal move sequences to the client. This made the engine more complex, but made moves
  and undo instant on the client. It also sets the app up for online play, since players can't cheat or submit
  illegal moves.
- The brief emphasised a "working, well-built" core game, so I spent most of my time on the engine and
  deployment. That meant leaving out features like player names, drag-and-drop checkers and a multi-game
  scoreboard, which brings me to…

## If I Had More Time

- Currently, the app is developed to easily accommodate future online play, so that would be the first thing
  I'd add.
- Along with it: accounts, an Elo system, a game replay viewer (via the move log) and end-to-end
  testing/production hardening.

## Time breakdown

| Task               | Time      |
|--------------------|-----------|
| Planning / design  | 1h        |
| Game logic + tests | 1.25h     |
| UI                 | 1h        |
| Infrastructure     | 0.75h     |
| README / notes     | 0.5h      |
| **Total**          | **4.5h**  |
