package com.tavli.engine;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

public final class RandomDice implements Dice {

    private final RandomGenerator random;

    public RandomDice() {
        this(new SecureRandom());
    }

    public RandomDice(RandomGenerator random) {
        this.random = random;
    }

    @Override
    public int roll() {
        return random.nextInt(1, 7);
    }
}
