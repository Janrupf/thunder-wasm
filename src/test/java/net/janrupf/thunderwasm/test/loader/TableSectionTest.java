package net.janrupf.thunderwasm.test.loader;

import net.janrupf.thunderwasm.module.WasmModule;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.module.section.TableSection;
import net.janrupf.thunderwasm.test.TestUtil;
import net.janrupf.thunderwasm.types.ReferenceType;
import net.janrupf.thunderwasm.types.TableType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TableSectionTest {
    @Test
    public void testTableSection() throws Exception {
        WasmModule module = TestUtil.load("table-section.wasm");

        TableSection section = TestUtil.getSection(module, (byte) 4);
        LargeArray<TableType> tables = section.getTypes();

        Assertions.assertEquals(1, tables.length());

        TableType tableType = tables.get(LargeArrayIndex.ZERO);
        Assertions.assertEquals(ReferenceType.FUNCREF, tableType.getElementType());
        Assertions.assertEquals(1, tableType.getLimits().getMin());
        Assertions.assertEquals(12, tableType.getLimits().getMax());
    }
}
