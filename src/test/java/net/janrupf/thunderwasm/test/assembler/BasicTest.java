package net.janrupf.thunderwasm.test.assembler;

import net.janrupf.thunderwasm.ThunderWasmException;
import net.janrupf.thunderwasm.assembler.WasmAssembler;
import net.janrupf.thunderwasm.module.WasmModule;
import net.janrupf.thunderwasm.runtime.linker.RuntimeLinker;
import net.janrupf.thunderwasm.runtime.linker.global.LinkedGlobal;
import net.janrupf.thunderwasm.runtime.linker.global.LinkedIntGlobal;
import net.janrupf.thunderwasm.test.TestUtil;
import net.janrupf.thunderwasm.types.ValueType;
import org.junit.jupiter.api.Test;

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
    public void playground() throws Exception {
        WasmModule module = TestUtil.load("assembler/simple.wasm");
        WasmAssembler assembler = TestUtil.makeAssembler(module);

        byte[] classBytes = assembler.assembleToModule();

        Files.write(Paths.get("/tmp/playground.class"), classBytes);

        Object moduleInstance = TestUtil.instantiateModule(assembler, classBytes, new TestLinker());
        Object result = TestUtil.callCodeMethod(
                moduleInstance,
                0,
                new Class<?>[] { int.class },
                new Object[] {
                        69
                }
        );

        System.out.println("$code_0() = " + result);
    }

    private static final class TestLinker implements RuntimeLinker {

        @Override
        public LinkedGlobal linkGlobal(String moduleName, String importName, ValueType type, boolean readOnly) throws ThunderWasmException {
            if (moduleName.equals("env") && importName.equals("test-global")) {
                return new SlotIntGlobal();
            }

            throw new ThunderWasmException("No such global " + moduleName + "::" + importName);
        }
    }

    private static final class SlotIntGlobal implements LinkedIntGlobal {
        private int value = 420;

        @Override
        public void set(int value) {
            this.value = value;
        }

        @Override
        public int get() {
            return this.value;
        }
    }
}
