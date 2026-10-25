/**
 * Rules engine for Tavli Portes. Pure Java: no Spring or other framework imports.
 *
 * <h2>Coordinates</h2>
 * Points are numbered 1..24 in absolute terms. {@link com.tavli.engine.Player#WHITE WHITE} moves from 24
 * towards 1 and has its home board on 1..6; {@link com.tavli.engine.Player#BLACK BLACK} moves from 1 towards
 * 24 and has its home board on 19..24. Internally, move generation works in <em>pips</em>: the distance from
 * the mover's bear-off edge, so that every player moves from high pips to low ones (see {@link
 * com.tavli.engine.Board#point(Player, int)}).
 *
 * <h2>Rule decisions</h2>
 * <ul>
 *   <li>Players roll their own dice: nothing is rolled until the player to act asks (see {@link
 *       com.tavli.engine.Phase}). In the opening each player throws one die, WHITE first; ties are thrown again,
 *       and the higher thrower plays both opening dice as their first roll.</li>
 *   <li>If no move is possible, the only legal turn is the empty one; the UI offers a "pass".</li>
 *   <li>A checker hit while its owner is bearing off must re-enter and come home before bearing off resumes.</li>
 *   <li>Bearing off with a die higher than the highest occupied point is permitted, not mandatory.</li>
 *   <li>The game ends as soon as the 15th checker is borne off; any unused dice are forfeited. A sequence
 *       that bears off the last checker is legal even if another sequence would use more dice.</li>
 * </ul>
 */
package com.tavli.engine;
