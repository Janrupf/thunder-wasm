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

import java.nio.ByteBuffer;
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
        WasmModule module = TestUtil.load("assembler/simple.wasm");
        WasmAssembler assembler = TestUtil.makeAssembler(module);

        byte[] classBytes = assembler.assembleToModule();

        Files.write(Paths.get("/tmp/playground.class"), classBytes);

        Table<?> table = new Table<>(0, 10);
        LinkedMemory memory = new LinkedMemory.Simple(new Limits(1, 20));

        Object moduleInstance = TestUtil.instantiateModule(assembler, classBytes, new TestLinker(table, memory));
        TestUtil.callCodeMethod(
                moduleInstance,
                0,
                new Class<?>[] {  },
                new Object[] {  }
        );

        Function<Integer, Byte> readByte = (address) -> {
            try {
                return (byte) (int) TestUtil.callCodeMethod(
                        moduleInstance,
                        1,
                        new Class<?>[] { int.class, },
                        new Object[] { address }
                );
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        };

        System.out.println("Read byte at 100: " + (char) (byte) readByte.apply(100));
        System.out.println("Read byte at 101: " + (char) (byte) readByte.apply(101));
        System.out.println("Read byte at 102: " + (char) (byte) readByte.apply(102));
        System.out.println("Read byte at 103: " + (char) (byte) readByte.apply(103));
        System.out.println("Read byte at  50: " + (char) (byte) readByte.apply(50));
        System.out.println("Read byte at  51: " + (char) (byte) readByte.apply(51));
        System.out.println("Read byte at  52: " + (char) (byte) readByte.apply(52));
        System.out.println("Read byte at  53: " + (char) (byte) readByte.apply(53));
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
