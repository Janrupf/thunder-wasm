package net.janrupf.thunderwasm.test.array;

import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LargeArrayIndexTest {
    private static void check(long largeIndex, int arrayIndex, int elementIndex) {
        LargeArrayIndex i = LargeArrayIndex.fromU64(largeIndex);

        Assertions.assertEquals(arrayIndex, i.getArrayIndex());
        Assertions.assertEquals(elementIndex, i.getElementIndex());
    }

    @Test
    public void testSmallIndices() {
        // All indices from 0 to MAX_VALUE - 1 should be mapped 1 to 1
        for (int i = 0; i != Integer.MAX_VALUE; i++) {
            check(i, 0, i);
        }
    }

    @Test
    public void testIntMaxValueIndices() {
        for (long i = Integer.MAX_VALUE; i != Integer.MAX_VALUE * 2L; i++) {
            check(i, 1, (int) (i - Integer.MAX_VALUE));
        }
    }

    @Test
    public void testIntMaxValuesPlusOneIndices() {
        for (long i = Integer.MAX_VALUE * 2L; i != Integer.MAX_VALUE * 3L; i++) {
            check(i, 2, (int) (i - Integer.MAX_VALUE * 2L));
        }
    }
}
