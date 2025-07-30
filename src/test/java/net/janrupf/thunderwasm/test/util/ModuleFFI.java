package net.janrupf.thunderwasm.test.util;

import net.janrupf.thunderwasm.runtime.WasmModuleExports;
import net.janrupf.thunderwasm.runtime.linker.function.LinkedFunction;
import net.janrupf.thunderwasm.runtime.linker.memory.LinkedMemory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class ModuleFFI {
    private final Object module;
    private final LinkedFunction alloc;
    private final LinkedFunction dealloc;
    private final LinkedMemory memory;
    private final LinkedFunction compilePattern;
    private final LinkedFunction disposePattern;
    private final LinkedFunction matchCount;

    public ModuleFFI(Object module) {
        this.module = module;

        Map<String, Object> exports = ((WasmModuleExports) module).getExports();

        this.alloc = (LinkedFunction) exports.get("alloc");
        this.dealloc = (LinkedFunction) exports.get("dealloc");
        this.memory = (LinkedMemory) exports.get("memory");
        this.compilePattern = (LinkedFunction) exports.get("compile_pattern");
        this.disposePattern = (LinkedFunction) exports.get("dispose_pattern");
        this.matchCount = (LinkedFunction) exports.get("match_count");
    }

    public SizedPtr alloc(int size) {
        try {
            int address = (int) alloc.asMethodHandle().invoke(size);
            return new SizedPtr(address, size);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void free(SizedPtr sizedPtr) {
        try {
            dealloc.asMethodHandle().invoke(sizedPtr.getAddress(), sizedPtr.getSize());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public SizedPtr allocateStringUTF8(String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);

        SizedPtr sizedPtr = alloc(bytes.length);
        ByteBuffer memoryBuffer = memory.asInternal();
        memoryBuffer.position(sizedPtr.getAddress()).put(bytes);

        return sizedPtr;
    }

    public int compilePattern(String pattern) {
        try {
            SizedPtr strPtr = allocateStringUTF8(pattern);
            return (int) this.compilePattern.asMethodHandle().invoke(strPtr.getAddress(), strPtr.getSize());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void disposePattern(int patternPtr) {
        try {
            this.disposePattern.asMethodHandle().invoke(patternPtr);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public int matchCount(int patternPtr, String s) {
        try {
            SizedPtr strPtr = allocateStringUTF8(s);
            return (int) this.matchCount.asMethodHandle().invoke(patternPtr, strPtr.getAddress(), strPtr.getSize());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static class SizedPtr {
        private final int address;
        private final int size;

        public SizedPtr(int address, int size) {
            this.address = address;
            this.size = size;
        }

        public int getAddress() {
            return address;
        }

        public int getSize() {
            return size;
        }
    }
}
