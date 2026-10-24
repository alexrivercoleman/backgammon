package com.tavli.config;

import com.tavli.engine.Dice;
import com.tavli.engine.RandomDice;
import com.tavli.engine.TavliEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class EngineConfig {

    @Bean
    Dice dice() {
        return new RandomDice();
    }

    @Bean
    TavliEngine tavliEngine(Dice dice) {
        return new TavliEngine(dice);
    }
}
