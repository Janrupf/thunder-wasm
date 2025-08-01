package net.janrupf.thunderwasm.test.assembler;

import net.janrupf.thunderwasm.ThunderWasmException;
import net.janrupf.thunderwasm.assembler.WasmAssembler;
import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.data.Limits;
import net.janrupf.thunderwasm.module.WasmModule;
import net.janrupf.thunderwasm.runtime.Table;
import net.janrupf.thunderwasm.runtime.linker.RuntimeLinker;
import net.janrupf.thunderwasm.runtime.linker.function.LinkedFunction;
import net.janrupf.thunderwasm.runtime.linker.global.LinkedGlobal;
import net.janrupf.thunderwasm.runtime.linker.global.LinkedObjectGlobal;
import net.janrupf.thunderwasm.runtime.linker.memory.LinkedMemory;
import net.janrupf.thunderwasm.runtime.linker.table.LinkedTable;
import net.janrupf.thunderwasm.test.util.ModuleFFI;
import net.janrupf.thunderwasm.test.util.TestUtil;
import net.janrupf.thunderwasm.types.FunctionType;
import net.janrupf.thunderwasm.types.ReferenceType;
import net.janrupf.thunderwasm.types.ValueType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Files;
import java.nio.file.Paths;

public class BasicTest {
    @Test
    public void testAssemblesSimpleSuccessfully() throws Exception {
        WasmModule module = TestUtil.load("assembler/simple.wasm");
        WasmAssembler assembler = TestUtil.makeAssembler(module);

        assembler.assembleToModule();
    }

    @Test
    public void playground() throws Throwable {
        long loadStartTime = System.currentTimeMillis();
        WasmModule module = TestUtil.load("testsuite-88e97b0f742f4c3ee01fea683da130f344dd7b02/table_fill.0.wasm");
        // WasmModule module = TestUtil.load("assembler/simple.wasm");
        // WasmModule module = TestUtil.loadFromFile("/projects/public/asmble/examples/rust-regex/target/wasm32-unknown-unknown/debug/rust_regex.wasm");
        long loadEndTime = System.currentTimeMillis();

        System.out.println("Loading took " + (loadEndTime - loadStartTime) + "ms");

        WasmAssembler assembler = TestUtil.makeAssembler(module);

        long assemblyStartTime = System.currentTimeMillis();
        byte[] classBytes = null;
        try {
            classBytes = assembler.assembleToModule();
        } catch (Exception e) {
            long assemblyEndTime = System.currentTimeMillis();
            Assertions.fail("Assembling the module failed after " + (assemblyEndTime - assemblyStartTime) + "ms", e);
        }
        long assemblyEndTime = System.currentTimeMillis();

        System.out.println("Assembling took " + (assemblyEndTime - assemblyStartTime) + "ms");

        Files.write(Paths.get("/tmp/playground.class"), classBytes);

        Table<?> table = new Table<>(0, 10);
        LinkedMemory memory = new LinkedMemory.Simple(new Limits(1, 64));

        Object moduleInstance = TestUtil.instantiateModule(assembler, classBytes, new TestLinker(table, memory));
        ModuleFFI ffi = new ModuleFFI(moduleInstance);

        int patternPtr = ffi.compilePattern("Hello");
        System.out.println("Pattern compiled at " + patternPtr);
        System.out.println("Match count: " + ffi.matchCount(patternPtr, "Hello World, Hello Universe!"));
        ffi.disposePattern(patternPtr);
    }

    private static final class TestLinker implements RuntimeLinker {
        private final Table<?> table;
        private final LinkedMemory memory;
        private final TestFunctionImplementations functions;

        public TestLinker(Table<?> table, LinkedMemory memory) {
            this.table = table;
            this.memory = memory;
            this.functions = new TestFunctionImplementations();
        }

        @Override
        public LinkedGlobal linkGlobal(String moduleName, String importName, ValueType type, boolean readOnly) {
            return new SlotExternrefGlobal();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> LinkedTable<T> linkTable(String moduleName, String importName, ReferenceType type, Limits limits) throws ThunderWasmException {
            return (LinkedTable<T>) table;
        }

        @Override
        public LinkedMemory linkMemory(String moduleName, String importName, Limits limits) {
            return memory;
        }

        @Override
        public LinkedFunction linkFunction(String moduleName, String importName, FunctionType type) throws ThunderWasmException {
            switch (importName) {
                case "hello":
                case "func":
                    return functions.hello;

                default:
                    throw new ThunderWasmException("No such import function " + importName);
            }
        }
    }

    private static final class SlotExternrefGlobal implements LinkedObjectGlobal<String> {
        private String value = "Hello, World!";

        @Override
        public String get() {
            return value;
        }

        @Override
        public void set(String value) {
            this.value = value;
        }

        @Override
        public ValueType getType() {
            return ReferenceType.EXTERNREF;
        }
    }

    private static final class TestFunctionImplementations {
        private final LinkedFunction hello;

        public TestFunctionImplementations() {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            try {
                this.hello = LinkedFunction.Simple.inferFromMethodHandle(
                        lookup.findVirtual(TestFunctionImplementations.class, "hello", MethodType.methodType(double.class, int.class, double.class))
                                .bindTo(this)
                );
            } catch (WasmAssemblerException | NoSuchMethodException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        private double hello(int a, double b) {
            System.out.println("Hello from WASM callback, a = " + a + ", b = " + b);
            return a * b;
        }
    }
}
