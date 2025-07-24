package net.janrupf.thunderwasm.test.array;

import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.ValueType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LargeArrayValueTest {
    @Test
    public void largeArrayEquals() {
        LargeArray<ValueType> a = new LargeArray<>(ValueType.class, LargeArrayIndex.ZERO.add(2));
        a.set(LargeArrayIndex.ZERO, NumberType.I32);
        a.set(LargeArrayIndex.ZERO.add(1), NumberType.F32);

        LargeArray<ValueType> b = new LargeArray<>(ValueType.class, LargeArrayIndex.ZERO.add(2));
        b.set(LargeArrayIndex.ZERO, NumberType.I32);
        b.set(LargeArrayIndex.ZERO.add(1), NumberType.F32);

        Assertions.assertEquals(a, b);
    }
}
