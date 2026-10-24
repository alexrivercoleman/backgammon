-- Current state of each game. `state` holds the engine state as versioned JSON; `status` and `winner`
-- duplicate parts of it so games can be queried without parsing JSON. `turn_number` increases with every
-- turn and guards against the same turn being applied twice.
create table games (
    id          uuid primary key,
    status      text        not null check (status in ('IN_PROGRESS', 'FINISHED')),
    winner      text        check (winner in ('WHITE', 'BLACK')),
    turn_number integer     not null,
    state       jsonb       not null,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now(),
    check ((status = 'FINISHED') = (winner is not null))
);

-- Append-only log of every dice throw and turn, in order (`seq`) per game.
--   OPENING_THROW: die1 = WHITE's die, die2 = BLACK's die (ties included)
--   ROLL:          the dice given to `player` for turn `turn_number`
--   TURN:          the moves `player` made in turn `turn_number`; an empty array is a pass
create table moves (
    id          bigint generated always as identity primary key,
    game_id     uuid        not null references games (id),
    seq         integer     not null,
    turn_number integer,
    kind        text        not null check (kind in ('OPENING_THROW', 'ROLL', 'TURN')),
    player      text        check (player in ('WHITE', 'BLACK')),
    die1        smallint    check (die1 between 1 and 6),
    die2        smallint    check (die2 between 1 and 6),
    moves       jsonb,
    created_at  timestamptz not null default now(),
    unique (game_id, seq),
    check (case kind
        when 'OPENING_THROW' then turn_number is null and player is null
                                  and die1 is not null and die2 is not null and moves is null
        when 'ROLL'          then turn_number is not null and player is not null
                                  and die1 is not null and die2 is not null and moves is null
        when 'TURN'          then turn_number is not null and player is not null
                                  and die1 is null and die2 is null and moves is not null
    end)
);

create function reject_moves_change() returns trigger
    language plpgsql as
$$
begin
    raise exception 'moves is append-only';
end;
$$;

create trigger moves_append_only
    before update or delete on moves
    for each row execute function reject_moves_change();

create trigger moves_no_truncate
    before truncate on moves
    for each statement execute function reject_moves_change();
