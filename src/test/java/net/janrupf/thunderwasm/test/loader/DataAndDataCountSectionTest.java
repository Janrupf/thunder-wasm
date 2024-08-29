package net.janrupf.thunderwasm.test.loader;

import net.janrupf.thunderwasm.module.WasmModule;
import net.janrupf.thunderwasm.module.section.DataCountSection;
import net.janrupf.thunderwasm.module.section.DataSection;
import net.janrupf.thunderwasm.module.section.segment.DataSegment;
import net.janrupf.thunderwasm.module.section.segment.DataSegmentMode;
import net.janrupf.thunderwasm.test.TestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

public class DataAndDataCountSectionTest {
    @Test
    public void testDataSection() throws Exception {
        WasmModule module = TestUtil.load("data-section.wasm");

        DataSection dataSection = TestUtil.getSection(module, (byte) 11);
        DataSegment[] segments = dataSection.getSegments().asFlatArray();

        Assertions.assertEquals(2, segments.length);

        DataSegment firstSegment = segments[0];
        Assertions.assertArrayEquals("Hello, World!".getBytes(StandardCharsets.UTF_8), firstSegment.getInit().asFlatArray());
        Assertions.assertInstanceOf(DataSegmentMode.Active.class, firstSegment.getMode());

        DataSegmentMode.Active firstActiveMode = (DataSegmentMode.Active) firstSegment.getMode();
        Assertions.assertEquals(0, firstActiveMode.getMemoryIndex());
        Assertions.assertEquals(1, firstActiveMode.getMemoryOffset().getInstructions().size());

        DataSegment secondSegment = segments[1];
        Assertions.assertArrayEquals("ABC".getBytes(StandardCharsets.UTF_8), secondSegment.getInit().asFlatArray());
        Assertions.assertInstanceOf(DataSegmentMode.Passive.class, secondSegment.getMode());
    }

    @Test
    public void testDataCountSection() throws Exception {
        WasmModule module = TestUtil.load("data-count-section.wasm");

        DataSection dataSection = TestUtil.getSection(module, (byte) 11);
        DataCountSection dataCountSection = TestUtil.getSection(module, (byte) 12);

        Assertions.assertEquals(dataSection.getSegments().length(), ((long) dataCountSection.getCount()) & 0xFFFFFFFFL);
    }
}
