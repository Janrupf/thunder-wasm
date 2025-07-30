package net.janrupf.thunderwasm.test.loader;

import net.janrupf.thunderwasm.module.WasmModule;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.module.section.MemorySection;
import net.janrupf.thunderwasm.test.util.TestUtil;
import net.janrupf.thunderwasm.types.MemoryType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MemorySectionTest {
    @Test
    public void testMemorySection() throws Exception {
        WasmModule module = TestUtil.load("memory-section.wasm");

        MemorySection section = TestUtil.getSection(module, (byte) 5);
        LargeArray<MemoryType> memories = section.getTypes();

        Assertions.assertEquals(1, memories.length());

        MemoryType memoryType = memories.get(LargeArrayIndex.ZERO);
        Assertions.assertEquals(1, memoryType.getLimits().getMin());
        Assertions.assertEquals(12, memoryType.getLimits().getMax());
    }
}
