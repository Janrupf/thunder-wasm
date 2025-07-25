package net.janrupf.thunderwasm.test.wast;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.janrupf.thunderwasm.test.wast.command.WastCommand;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public class WastTestSuite {
    private static final Set<String> UNSUPPORTED = new HashSet<>();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        UNSUPPORTED.add("simd_");
    }

    // Ideally this would run the manifests concurrently, but this currently is not
    // possible. See here: https://github.com/junit-team/junit-framework/issues/2497.
    //
    // For now just run everything sequentially, its slower, but hey, we are "just"
    // dealing with tests either way.
    @TestFactory
    @Execution(ExecutionMode.SAME_THREAD)
    public Stream<DynamicContainer> wastTests() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(WastTestSuite.class.getResourceAsStream("/wast-tests.txt"))
        ));

        return reader.lines()
                .map((line) -> {
                    String basePath = line.substring(0, line.lastIndexOf('/'));
                    String name = line.substring(line.lastIndexOf('/') + 1, line.lastIndexOf('.'));

                    try {
                        WastManifest manifest = OBJECT_MAPPER.readValue(
                                WastTestSuite.class.getResource("/" + line),
                                WastManifest.class
                        );

                        return new WastTestCollection(manifest, name, basePath);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .map(this::processCollection);
    }

    private DynamicContainer processCollection(WastTestCollection collection) throws UncheckedIOException {
        // Create a test executor with runtime linker
        WasmTestExecutor executor = new WasmTestExecutor(collection);

        return DynamicContainer.dynamicContainer(
                collection.getName(),
                collection.getManifest().getCommands().stream()
                        .map(command -> createTestForCommand(command, executor))
        );
    }

    /**
     * Creates a JUnit dynamic test for a single WAST command.
     */
    private DynamicTest createTestForCommand(WastCommand command, WasmTestExecutor executor) {
        String displayMeta = "";
        if (command.getTestDisplayMeta() != null) {
            displayMeta = " [" + command.getTestDisplayMeta() + "]";
        }

        String testName = String.format("%s (line %d)%s",
                command.getClass().getSimpleName().replace("Command", ""),
                command.getLine(),
                displayMeta
        );
        return DynamicTest.dynamicTest(testName, () -> runTestCommand(command, executor));
    }

    private void runTestCommand(WastCommand command, WasmTestExecutor executor) throws Throwable {
        for (String unsupported : UNSUPPORTED) {
            if (executor.getCollection().getName().startsWith(unsupported)) {
                Assumptions.abort("Unsupported by category " + unsupported);
            }
        }

        Assumptions.assumeFalse(executor.isBroken(), "Previous test has failed");
        executor.executeCommand(command);
    }
}
