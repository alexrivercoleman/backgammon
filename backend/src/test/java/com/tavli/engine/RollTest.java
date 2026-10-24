package com.tavli.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RollTest {

    @Test
    void nonDoubleGivesBothDice() {
        Roll roll = new Roll(3, 5);

        assertThat(roll.isDouble()).isFalse();
        assertThat(roll.dice()).containsExactly(3, 5);
        assertThat(roll.higher()).isEqualTo(5);
        assertThat(roll.lower()).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    void doublesGiveFourMoves(int value) {
        Roll roll = new Roll(value, value);

        assertThat(roll.isDouble()).isTrue();
        assertThat(roll.dice()).containsExactly(value, value, value, value);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 7, -1})
    void rejectsInvalidDieValues(int value) {
        assertThatThrownBy(() -> new Roll(value, 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rollsTwoDiceFromTheSource() {
        int[] values = {4, 2};
        int[] next = {0};

        assertThat(Roll.of(() -> values[next[0]++])).isEqualTo(new Roll(4, 2));
    }

    @Test
    void randomDiceStaysInRange() {
        Dice dice = new RandomDice();
        for (int i = 0; i < 1000; i++) {
            assertThat(dice.roll()).isBetween(1, 6);
        }
    }
}
