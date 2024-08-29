package net.janrupf.thunderwasm.test.loader;

import net.janrupf.thunderwasm.data.Global;
import net.janrupf.thunderwasm.exports.*;
import net.janrupf.thunderwasm.module.WasmModule;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.module.section.*;
import net.janrupf.thunderwasm.test.TestUtil;
import net.janrupf.thunderwasm.types.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExportSectionTest {
    @Test
    public void testExportSection() throws Exception {
        WasmModule module = TestUtil.load("export-section.wasm");

        TypeSection typeSection = TestUtil.getSection(module, (byte) 1);
        FunctionSection functionSection = TestUtil.getSection(module, (byte) 3);
        TableSection tableSection = TestUtil.getSection(module, (byte) 4);
        MemorySection memorySection = TestUtil.getSection(module, (byte) 5);
        GlobalSection globalSection = TestUtil.getSection(module, (byte) 6);
        ExportSection exportSection = TestUtil.getSection(module, (byte) 7);
        LargeArray<Export> exports = exportSection.getExports();

        Assertions.assertEquals(4, exports.length());

        // Test function export
        {
            Assertions.assertEquals("function", exports.get(LargeArrayIndex.ZERO).getName());
            Assertions.assertInstanceOf(FunctionExportDescription.class, exports.get(LargeArrayIndex.ZERO).getDescription());

            FunctionExportDescription exportDescription = (FunctionExportDescription) exports.get(LargeArrayIndex.ZERO).getDescription();
            int functionTypeIndex = functionSection.getTypes().get(LargeArrayIndex.fromU32(exportDescription.getIndex()));
            FunctionType fnType = typeSection.getTypes().get(LargeArrayIndex.fromU32(functionTypeIndex));

            TestUtil.checkFunctionType(fnType, new ValueType[]{NumberType.I32}, new ValueType[]{NumberType.I32});
        }

        // Test memory export
        {
            Assertions.assertEquals("memory", exports.get(LargeArrayIndex.ZERO.add(1)).getName());
            Assertions.assertInstanceOf(MemoryExportDescription.class, exports.get(LargeArrayIndex.ZERO.add(1)).getDescription());

            MemoryExportDescription exportDescription = (MemoryExportDescription) exports.get(LargeArrayIndex.ZERO.add(1)).getDescription();
            MemoryType memoryType = memorySection.getTypes().get(LargeArrayIndex.fromU32(exportDescription.getIndex()));

            Assertions.assertEquals(1, memoryType.getLimits().getMin());
            Assertions.assertEquals(12, memoryType.getLimits().getMax());
        }

        // Test table export
        {
            Assertions.assertEquals("table", exports.get(LargeArrayIndex.ZERO.add(2)).getName());
            Assertions.assertInstanceOf(TableExportDescription.class, exports.get(LargeArrayIndex.ZERO.add(2)).getDescription());

            TableExportDescription exportDescription = (TableExportDescription) exports.get(LargeArrayIndex.ZERO.add(2)).getDescription();
            TableType tableType = tableSection.getTypes().get(LargeArrayIndex.fromU32(exportDescription.getIndex()));

            Assertions.assertEquals(ReferenceType.FUNCREF, tableType.getElementType());
            Assertions.assertEquals(1, tableType.getLimits().getMin());
            Assertions.assertEquals(12, tableType.getLimits().getMax());
        }

        // Test global export
        {
            Assertions.assertEquals("global", exports.get(LargeArrayIndex.ZERO.add(3)).getName());
            Assertions.assertInstanceOf(GlobalExportDescription.class, exports.get(LargeArrayIndex.ZERO.add(3)).getDescription());

            GlobalExportDescription exportDescription = (GlobalExportDescription) exports.get(LargeArrayIndex.ZERO.add(3)).getDescription();
            Global global = globalSection.getGlobals().get(LargeArrayIndex.fromU32(exportDescription.getIndex()));

            Assertions.assertEquals(NumberType.I32, global.getType().getValueType());
            Assertions.assertEquals(GlobalType.Mutability.VAR, global.getType().getMutability());
        }
    }
}
