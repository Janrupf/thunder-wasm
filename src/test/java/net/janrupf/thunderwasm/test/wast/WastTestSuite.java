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
import java.util.*;
import java.util.stream.Stream;

public class WastTestSuite {
    private static final class TestIdent {
        private final String collection;
        private final int line;

        public TestIdent(String collection, int line) {
            this.collection = collection;
            this.line = line;
        }

        public String getCollection() {
            return collection;
        }

        public int getLine() {
            return line;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof TestIdent)) return false;
            TestIdent testIdent = (TestIdent) o;
            return line == testIdent.line && Objects.equals(collection, testIdent.collection);
        }

        @Override
        public int hashCode() {
            return Objects.hash(collection, line);
        }
    }

    private static final Set<String> UNSUPPORTED_CATEGORIES = new HashSet<>();
    private static final Map<TestIdent, String> KNOWN_BROKEN_TESTS = new HashMap<>();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        UNSUPPORTED_CATEGORIES.add("simd_");

        // We already support multiple memories, but the tests are written with the
        // assumption that there is only one memory.
        KNOWN_BROKEN_TESTS.put(new TestIdent("binary", 126), "Assumes single memory");
        KNOWN_BROKEN_TESTS.put(new TestIdent("binary", 146), "Assumes single memory");
        KNOWN_BROKEN_TESTS.put(new TestIdent("binary", 166), "Assumes single memory");
        KNOWN_BROKEN_TESTS.put(new TestIdent("binary", 185), "Assumes single memory");
        KNOWN_BROKEN_TESTS.put(new TestIdent("binary", 204), "Assumes single memory");
        KNOWN_BROKEN_TESTS.put(new TestIdent("binary", 224), "Assumes single memory");
        KNOWN_BROKEN_TESTS.put(new TestIdent("binary", 243), "Assumes single memory");
        KNOWN_BROKEN_TESTS.put(new TestIdent("binary", 262), "Assumes single memory");
        KNOWN_BROKEN_TESTS.put(new TestIdent("binary", 280), "Assumes single memory");
        KNOWN_BROKEN_TESTS.put(new TestIdent("binary", 298), "Assumes single memory");
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
        for (String unsupported : UNSUPPORTED_CATEGORIES) {
            if (executor.getCollection().getName().startsWith(unsupported)) {
                Assumptions.abort("Unsupported by category " + unsupported);
            }
        }

        TestIdent ident = new TestIdent(executor.getCollection().getName(), command.getLine());
        if (KNOWN_BROKEN_TESTS.containsKey(ident)) {
            Assumptions.abort("Known broken test: " + KNOWN_BROKEN_TESTS.get(ident));
        }

        Assumptions.assumeFalse(executor.isBroken(), "Previous test has failed");
        executor.executeCommand(command);
    }
}
