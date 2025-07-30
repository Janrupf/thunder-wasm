package net.janrupf.thunderwasm.test.loader;

import net.janrupf.thunderwasm.module.WasmModule;
import net.janrupf.thunderwasm.module.section.StartSection;
import net.janrupf.thunderwasm.test.util.TestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StartSectionTest {
    @Test
    public void testStartSection() throws Exception {
        WasmModule module = TestUtil.load("start-section.wasm");

        StartSection section = TestUtil.getSection(module, (byte) 8);
        Assertions.assertEquals(0, section.getIndex());
    }
}
