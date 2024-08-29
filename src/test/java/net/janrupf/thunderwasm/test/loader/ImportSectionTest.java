package net.janrupf.thunderwasm.test.loader;

import net.janrupf.thunderwasm.imports.*;
import net.janrupf.thunderwasm.module.WasmModule;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.module.section.ImportSection;
import net.janrupf.thunderwasm.module.section.TypeSection;
import net.janrupf.thunderwasm.test.TestUtil;
import net.janrupf.thunderwasm.types.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ImportSectionTest {
    @Test
    public void testImportSection() throws Exception {
        WasmModule module = TestUtil.load("import-section.wasm");

        TypeSection typeSection = TestUtil.getSection(module, (byte) 1);

        ImportSection section = TestUtil.getSection(module, (byte) 2);
        LargeArray<Import> imports = section.getImports();

        /*
            (module
                (import "someModule" "someFunction" (func $someFunction (param i32) (result i32)))
                (import "someModule" "memory" (memory 1 12))
                (import "someModule" "table" (table 1 12 funcref))
                (import "someModule" "global" (global $someGlobal (mut i32)))
            )
        */
        Assertions.assertEquals(4, imports.length());

        // Test function import
        {
            Assertions.assertEquals("someModule", imports.get(LargeArrayIndex.ZERO).getModule());
            Assertions.assertEquals("someFunction", imports.get(LargeArrayIndex.ZERO).getName());
            Assertions.assertInstanceOf(TypeImportDescription.class, imports.get(LargeArrayIndex.ZERO).getDescription());

            TypeImportDescription importDescription = (TypeImportDescription) imports.get(LargeArrayIndex.ZERO).getDescription();
            FunctionType fnType = typeSection.getTypes().get(LargeArrayIndex.fromU32(importDescription.getIndex()));

            LargeArray<ValueType> inputs = fnType.getInputs();
            Assertions.assertEquals(1, inputs.length());
            Assertions.assertEquals(NumberType.I32, inputs.get(LargeArrayIndex.ZERO));

            LargeArray<ValueType> outputs = fnType.getOutputs();
            Assertions.assertEquals(1, outputs.length());
            Assertions.assertEquals(NumberType.I32, outputs.get(LargeArrayIndex.ZERO));
        }

        // Test memory import
        {
            Assertions.assertEquals("someModule", imports.get(LargeArrayIndex.ZERO.add(1)).getModule());
            Assertions.assertEquals("memory", imports.get(LargeArrayIndex.ZERO.add(1)).getName());
            Assertions.assertInstanceOf(MemoryImportDescription.class, imports.get(LargeArrayIndex.ZERO.add(1)).getDescription());

            MemoryImportDescription importDescription = (MemoryImportDescription) imports.get(LargeArrayIndex.ZERO.add(1)).getDescription();
            Assertions.assertEquals(1, importDescription.getType().getLimits().getMin());
            Assertions.assertEquals(12, importDescription.getType().getLimits().getMax());
        }

        // Test table import
        {
            Assertions.assertEquals("someModule", imports.get(LargeArrayIndex.ZERO.add(2)).getModule());
            Assertions.assertEquals("table", imports.get(LargeArrayIndex.ZERO.add(2)).getName());
            Assertions.assertInstanceOf(TableImportDescription.class, imports.get(LargeArrayIndex.ZERO.add(2)).getDescription());

            TableImportDescription importDescription = (TableImportDescription) imports.get(LargeArrayIndex.ZERO.add(2)).getDescription();
            Assertions.assertEquals(ReferenceType.FUNCREF, importDescription.getType().getElementType());
            Assertions.assertEquals(1, importDescription.getType().getLimits().getMin());
            Assertions.assertEquals(12, importDescription.getType().getLimits().getMax());
        }

        // Test global import
        {
            Assertions.assertEquals("someModule", imports.get(LargeArrayIndex.ZERO.add(3)).getModule());
            Assertions.assertEquals("global", imports.get(LargeArrayIndex.ZERO.add(3)).getName());
            Assertions.assertInstanceOf(GlobalImportDescription.class, imports.get(LargeArrayIndex.ZERO.add(3)).getDescription());

            GlobalImportDescription importDescription = (GlobalImportDescription) imports.get(LargeArrayIndex.ZERO.add(3)).getDescription();
            Assertions.assertEquals(NumberType.I32, importDescription.getType().getValueType());
            Assertions.assertEquals(GlobalType.Mutability.VAR, importDescription.getType().getMutability());
        }
    }
}
