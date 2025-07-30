package net.janrupf.thunderwasm.test.loader;

import net.janrupf.thunderwasm.data.Global;
import net.janrupf.thunderwasm.module.WasmModule;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.module.section.GlobalSection;
import net.janrupf.thunderwasm.test.util.TestUtil;
import net.janrupf.thunderwasm.types.GlobalType;
import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.ReferenceType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GlobalSectionTest {
    @Test
    public void testGlobalSection() throws Exception {
        WasmModule module = TestUtil.load("global-section.wasm");

        GlobalSection globalSection = TestUtil.getSection(module, (byte) 6);

        Assertions.assertEquals(2, globalSection.getGlobals().length());

        Global firstGlobal = globalSection.getGlobals().get(LargeArrayIndex.ZERO);
        Global secondGlobal = globalSection.getGlobals().get(LargeArrayIndex.ZERO.add(1));

        // First global
        Assertions.assertEquals(NumberType.I32, firstGlobal.getType().getValueType());
        Assertions.assertEquals(GlobalType.Mutability.VAR, firstGlobal.getType().getMutability());

        // Second global
        Assertions.assertEquals(ReferenceType.FUNCREF, secondGlobal.getType().getValueType());
        Assertions.assertEquals(GlobalType.Mutability.CONST, secondGlobal.getType().getMutability());
    }
}
