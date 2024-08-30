package net.janrupf.thunderwasm.test.assembler;

import net.janrupf.thunderwasm.ThunderWasmException;
import net.janrupf.thunderwasm.assembler.WasmAssembler;
import net.janrupf.thunderwasm.module.WasmModule;
import net.janrupf.thunderwasm.runtime.ExternReference;
import net.janrupf.thunderwasm.runtime.linker.RuntimeLinker;
import net.janrupf.thunderwasm.runtime.linker.global.LinkedGlobal;
import net.janrupf.thunderwasm.runtime.linker.global.LinkedIntGlobal;
import net.janrupf.thunderwasm.runtime.linker.global.LinkedObjectGlobal;
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
                new Class<?>[] { ExternReference.class },
                new Object[] { new ExternReference("Blub!") }
        );

        System.out.println("$code_0() = " + result);
    }

    private static final class TestLinker implements RuntimeLinker {

        @Override
        public LinkedGlobal linkGlobal(String moduleName, String importName, ValueType type, boolean readOnly) throws ThunderWasmException {
            if (moduleName.equals("env") && importName.equals("test-global")) {
                return new SlotExternrefGlobal();
            }

            throw new ThunderWasmException("No such global " + moduleName + "::" + importName);
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
