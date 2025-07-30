package net.janrupf.thunderwasm.test.loader;

import net.janrupf.thunderwasm.module.WasmModule;
import net.janrupf.thunderwasm.test.util.TestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EmptyTest {
    @Test
    public void testEmpty() throws Exception {
        WasmModule module = TestUtil.load("empty.wasm");

        Assertions.assertEquals(1, module.getVersion());
        Assertions.assertEquals(0, module.getSections().size());
    }
}
