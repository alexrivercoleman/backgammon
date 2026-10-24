package com.tavli.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** The engine must stay framework-free: its main sources may only import from the JDK. */
class EngineIsolationTest {

    private static final Path ENGINE_SOURCES = Path.of("src/main/java/com/tavli/engine");

    @Test
    void engineImportsOnlyJdkPackages() throws IOException {
        List<String> offending;
        try (Stream<Path> files = Files.walk(ENGINE_SOURCES)) {
            offending = files.filter(f -> f.toString().endsWith(".java"))
                    .flatMap(EngineIsolationTest::imports)
                    .filter(line -> !line.matches("import (static )?java\\..*"))
                    .toList();
        }

        assertThat(Files.isDirectory(ENGINE_SOURCES)).isTrue();
        assertThat(offending).isEmpty();
    }

    private static Stream<String> imports(Path file) {
        try {
            return Files.readAllLines(file).stream().map(String::strip).filter(l -> l.startsWith("import "));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
