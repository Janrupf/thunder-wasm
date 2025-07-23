package net.janrupf.thunderwasm.test.assembler;

import net.janrupf.thunderwasm.ThunderWasmException;
import net.janrupf.thunderwasm.assembler.WasmAssembler;
import net.janrupf.thunderwasm.data.Limits;
import net.janrupf.thunderwasm.module.WasmModule;
import net.janrupf.thunderwasm.runtime.ElementReference;
import net.janrupf.thunderwasm.runtime.ExternReference;
import net.janrupf.thunderwasm.runtime.Table;
import net.janrupf.thunderwasm.runtime.linker.RuntimeLinker;
import net.janrupf.thunderwasm.runtime.linker.global.LinkedGlobal;
import net.janrupf.thunderwasm.runtime.linker.global.LinkedObjectGlobal;
import net.janrupf.thunderwasm.runtime.linker.memory.LinkedMemory;
import net.janrupf.thunderwasm.runtime.linker.table.LinkedTable;
import net.janrupf.thunderwasm.test.TestUtil;
import net.janrupf.thunderwasm.types.ReferenceType;
import net.janrupf.thunderwasm.types.ValueType;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Function;

public class BasicTest {
    @Test
    public void testAssemblesSimpleSuccessfully() throws Exception {
        WasmModule module = TestUtil.load("assembler/simple.wasm");
        WasmAssembler assembler = TestUtil.makeAssembler(module);

        assembler.assembleToModule();
    }

    @Test
    public void playground() throws Exception {
        long loadStartTime = System.currentTimeMillis();
        WasmModule module = TestUtil.load("assembler/simple.wasm");
        long loadEndTime = System.currentTimeMillis();

        System.out.println("Loading took " + (loadEndTime - loadStartTime) + "ms");

        WasmAssembler assembler = TestUtil.makeAssembler(module);

        long assemblyStartTime = System.currentTimeMillis();
        byte[] classBytes = assembler.assembleToModule();
        long assemblyEndTime = System.currentTimeMillis();

        System.out.println("Assembling took " + (assemblyEndTime - assemblyStartTime) + "ms");

        Files.write(Paths.get("/tmp/playground.class"), classBytes);

        Table<?> table = new Table<>(0, 10);
        LinkedMemory memory = new LinkedMemory.Simple(new Limits(1, 20));

        Object moduleInstance = TestUtil.instantiateModule(assembler, classBytes, new TestLinker(table, memory));
        int result = (int) TestUtil.callCodeMethod(
                moduleInstance,
                2,
                new Class<?>[]{ int.class },
                new Object[]{ 1 }
        );

        System.out.println("Result: " + result);
    }

    private static final class TestLinker implements RuntimeLinker {
        private final Table<?> table;
        private final LinkedMemory memory;

        public TestLinker(Table<?> table, LinkedMemory memory) {
            this.table = table;
            this.memory = memory;
        }

        @Override
        public LinkedGlobal linkGlobal(String moduleName, String importName, ValueType type, boolean readOnly) throws ThunderWasmException {
            if (moduleName.equals("env") && importName.equals("test-global")) {
                return new SlotExternrefGlobal();
            }

            throw new ThunderWasmException("No such global " + moduleName + "::" + importName);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends ElementReference> LinkedTable<T> linkTable(String moduleName, String importName, ReferenceType type, Limits limits) throws ThunderWasmException {
            return (LinkedTable<T>) table;
        }

        @Override
        public LinkedMemory linkMemory(String moduleName, String importName, Limits limits) throws ThunderWasmException {
            return memory;
        }
    }

    private static final class SlotExternrefGlobal implements LinkedObjectGlobal<ExternReference> {
        private ExternReference value = new ExternReference("Hello, World!");

        @Override
        public ExternReference get() {
            return value;
        }

        @Override
        public void set(ExternReference value) {
            this.value = value;
        }
    }
}
