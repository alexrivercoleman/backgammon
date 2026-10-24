package com.tavli;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

/** Testcontainers Postgres plus scripted dice in place of random ones. */
@TestConfiguration(proxyBeanMethods = false)
@Import(TestcontainersConfiguration.class)
public class IntegrationTestConfiguration {

    @Bean
    @Primary
    ScriptedDice scriptedDice() {
        return new ScriptedDice();
    }
}
