package net.janrupf.thunderwasm.test.assembler;

import net.janrupf.thunderwasm.assembler.WasmAssembler;
import net.janrupf.thunderwasm.module.WasmModule;
import net.janrupf.thunderwasm.test.TestUtil;
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

        Object moduleInstance = TestUtil.instantiateModule(assembler, classBytes);
        Object result = TestUtil.callCodeMethod(
                moduleInstance,
                0,
                new Class<?>[] { float.class },
                new Object[] {
                        4294967040.0f
                }
        );

        System.out.println("$code_0() = " + Integer.toUnsignedString((int) result));
    }
}
