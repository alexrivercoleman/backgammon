package com.tavli;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** The probe endpoints Docker and Kubernetes rely on, and nothing else from Actuator. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationTestConfiguration.class)
class HealthIT {

    @Autowired
    MockMvcTester mvc;

    @Test
    void livenessIsUp() {
        assertThat(mvc.get().uri("/actuator/health/liveness").exchange())
                .hasStatusOk()
                .bodyJson().extractingPath("$.status").isEqualTo("UP");
    }

    @Test
    void readinessIsUpAndIncludesTheDatabase() {
        assertThat(mvc.get().uri("/actuator/health/readiness").exchange())
                .hasStatusOk()
                .bodyJson().satisfies(json -> {
                    assertThat(json).extractingPath("$.status").isEqualTo("UP");
                    assertThat(json).extractingPath("$.components.db.status").isEqualTo("UP");
                });
    }

    @Test
    void otherActuatorEndpointsAreNotExposed() {
        assertThat(mvc.get().uri("/actuator/env").exchange()).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.get().uri("/actuator/beans").exchange()).hasStatus(HttpStatus.NOT_FOUND);
    }
}
