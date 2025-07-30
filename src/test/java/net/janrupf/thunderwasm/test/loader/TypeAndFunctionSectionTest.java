package net.janrupf.thunderwasm.test.loader;

import net.janrupf.thunderwasm.module.WasmModule;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.module.section.FunctionSection;
import net.janrupf.thunderwasm.module.section.TypeSection;
import net.janrupf.thunderwasm.test.util.TestUtil;
import net.janrupf.thunderwasm.types.FunctionType;
import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.ReferenceType;
import net.janrupf.thunderwasm.types.ValueType;
import org.junit.jupiter.api.Test;

public class TypeAndFunctionSectionTest {
    @Test
    public void testTypeAndFunctionSection() throws Exception {
        WasmModule module = TestUtil.load("type-function-section.wasm");

        TypeSection typeSection = TestUtil.getSection(module, (byte) 1);
        FunctionSection functionSection = TestUtil.getSection(module, (byte) 3);

        // (param i32)
        // (result i32)
        int functionOneIdx = functionSection.getTypes().get(LargeArrayIndex.ZERO);
        FunctionType fnOneType = typeSection.getTypes().get(LargeArrayIndex.fromU32(functionOneIdx));
        TestUtil.checkFunctionType(fnOneType, new ValueType[]{NumberType.I32}, new ValueType[]{NumberType.I32});

        // (param i32)
        // (result i32)
        int functionTwoIdx = functionSection.getTypes().get(LargeArrayIndex.ZERO.add(1));
        FunctionType fnTwoType = typeSection.getTypes().get(LargeArrayIndex.fromU32(functionTwoIdx));
        TestUtil.checkFunctionType(fnTwoType, new ValueType[]{NumberType.I32}, new ValueType[]{NumberType.I32});

        //  (param i64)
        //  (param i32)
        //  (param i64)
        //  (param f64)
        //  (param externref)
        //  (param funcref)
        //  (result i32)
        //  (result i32)
        int functionThreeIdx = functionSection.getTypes().get(LargeArrayIndex.ZERO.add(2));
        FunctionType fnThreeType = typeSection.getTypes().get(LargeArrayIndex.fromU32(functionThreeIdx));

        TestUtil.checkFunctionType(
                fnThreeType,
                new ValueType[]{
                        NumberType.I64,
                        NumberType.I32,
                        NumberType.I64,
                        NumberType.F64,
                        ReferenceType.EXTERNREF,
                        ReferenceType.FUNCREF
                },
                new ValueType[]{NumberType.I32, NumberType.I32}
        );
    }

}
