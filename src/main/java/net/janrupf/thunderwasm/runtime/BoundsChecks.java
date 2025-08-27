package net.janrupf.thunderwasm.runtime;

@SuppressWarnings("unused") // used by generated code
public final class BoundsChecks {
    public static void checkMemoryBulkWrite(int d, int n, int memorySize) {
        long byteSize = (long) memorySize * 65536;
        if (byteSize > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Memory size too large");
        }

        checkBounds(n, d, (int) byteSize, "destination");
    }

    public static void checkMemoryCopyBulkAccess(int d, int s, int n, int sourceMemorySize, int destMemorySize) {
        long sourceByteSize = (long) sourceMemorySize * 65536;
        long destByteSize = (long) destMemorySize * 65536;
        if (sourceByteSize > Integer.MAX_VALUE || destByteSize > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Memory size too large");
        }

        checkBounds(n, s, (int) sourceByteSize, "source memory");
        checkBounds(n, d, (int) destByteSize, "destination memory");
    }

    public static void checkDataBulkAccess(int n, int dataSegmentSize) {
        if (n > dataSegmentSize) {
            throw new IndexOutOfBoundsException("Out of bounds data segment access");
        }
    }

    public static void checkTableBulkWrite(int d, int n, int tableSize) {
        checkBounds(n, d, tableSize, "destination table");
    }

    public static void checkTableCopyBulkAccess(int d, int s, int n, int sourceTableSize, int destTableSize) {
        checkBounds(n, s, sourceTableSize, "source table");
        checkBounds(n, d, destTableSize, "destination table");
    }

    public static void checkElementBulkAccess(int n, int dataSegmentSize) {
        if (n > dataSegmentSize) {
            throw new IndexOutOfBoundsException("Out of bounds element segment access");
        }
    }

    private static void checkBounds(int n, int offset, int size, String type) {
        if (n < 0 || offset < 0) {
            throw new IndexOutOfBoundsException("Negative " + type + " access parameters");
        }

        if (offset > size - n) {
            throw new IndexOutOfBoundsException("Out of bounds " + type + " access");
        }
    }

    public static int calculateEffectiveAddress(int base, int offset) {
        if ((base < 0 && base + offset >= 0) || (offset < 0 && base + offset >= 0)) {
            throw new IndexOutOfBoundsException("Memory address overflow");
        }

        return base + offset;
    }
}
