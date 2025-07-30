package net.janrupf.thunderwasm.test.loader;

import net.janrupf.thunderwasm.module.WasmModule;
import net.janrupf.thunderwasm.module.section.CustomSection;
import net.janrupf.thunderwasm.module.section.WasmSection;
import net.janrupf.thunderwasm.test.util.TestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

public class CustomSectionTest {
    @Test
    public void testCustomSection() throws Exception {
        WasmModule module = TestUtil.load("custom-section.wasm");

        Assertions.assertEquals(1, module.getSections().size());

        WasmSection section = module.getSections().get(0);
        Assertions.assertEquals((byte) 0, section.getId());
        Assertions.assertInstanceOf(CustomSection.class, section);

        CustomSection customSection = (CustomSection) section;
        Assertions.assertEquals("custom-section-one", customSection.getName());
        Assertions.assertEquals(
                "Hello, World!",
                new String(customSection.getData().asFlatArray(), StandardCharsets.UTF_8)
        );
    }
}
