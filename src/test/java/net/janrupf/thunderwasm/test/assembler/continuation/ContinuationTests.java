package net.janrupf.thunderwasm.test.assembler.continuation;

import net.janrupf.thunderwasm.assembler.WasmAssembler;
import net.janrupf.thunderwasm.assembler.WasmAssemblerConfiguration;
import net.janrupf.thunderwasm.module.WasmModule;
import net.janrupf.thunderwasm.runtime.WasmModuleExports;
import net.janrupf.thunderwasm.runtime.continuation.Continuation;
import net.janrupf.thunderwasm.runtime.linker.RuntimeLinker;
import net.janrupf.thunderwasm.runtime.linker.function.LinkedFunction;
import net.janrupf.thunderwasm.runtime.linker.memory.LinkedMemory;
import net.janrupf.thunderwasm.test.util.TestUtil;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class ContinuationTests {
    private static final WasmAssemblerConfiguration CONFIGURATION = WasmAssemblerConfiguration.builder()
            .enableContinuations()
            .build();

    @Test
    public void testEmptyFunction() throws Throwable {
        WasmModule module = TestUtil.load("assembler/continuation/empty-function.wasm");
        WasmAssembler assembler = TestUtil.makeAssembler(module, CONFIGURATION);

        TestUtil.instantiateModule(assembler, new RuntimeLinker.Empty());
    }

    @Test
    public void playground() throws Throwable {
        WasmModule module = TestUtil.loadFromFile("/projects/private/thunder-wasm-rust-playground/target/wasm32-unknown-unknown/debug/twrp.wasm");
        // WasmModule module = TestUtil.load("assembler/continuation/playground.wasm");
        // WasmModule module = TestUtil.loadFromFile("/projects/public/asmble/examples/rust-regex/target/wasm32-unknown-unknown/release/rust_regex.wasm");
        WasmAssembler assembler = TestUtil.makeAssembler(module, CONFIGURATION);

        byte[] classBytes = assembler.assembleToModule();
        Files.write(Paths.get("/tmp/playground.class"), classBytes);

        ContinuationLinker linker = new ContinuationLinker();
        Object instance = TestUtil.instantiateModule(assembler, classBytes, linker);
        System.out.println(instance);

        Map<String, Object> exports = ((WasmModuleExports) instance).getExports();
        linker.setMemory((LinkedMemory) exports.get("memory"));

        LinkedFunction run = (LinkedFunction) exports.get("run");

        AtomicLong startTime = new AtomicLong(System.nanoTime());
        Continuation continuation = new Continuation(() -> {
            long now = System.nanoTime();
            return now - startTime.get() > 500_000;
        });

        for (int i = 0; i < 100; i++) {
            continuation.unpause();
            startTime.set(System.nanoTime());
            long currentNanos = System.nanoTime();
            run.asMethodHandle().invoke(continuation);
            long nanosNow = System.nanoTime();

            System.out.println("Paused after " + (nanosNow - currentNanos) + " nanoseconds");
            Thread.sleep(10);
        }
    }
}
