package net.janrupf.thunderwasm.test.wast.linker;

import net.janrupf.thunderwasm.data.Limits;
import net.janrupf.thunderwasm.runtime.Table;
import net.janrupf.thunderwasm.runtime.linker.function.LinkedFunction;
import net.janrupf.thunderwasm.runtime.linker.global.*;
import net.janrupf.thunderwasm.runtime.linker.memory.LinkedMemory;
import net.janrupf.thunderwasm.runtime.linker.table.LinkedTable;
import net.janrupf.thunderwasm.types.FunctionType;
import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.ReferenceType;
import net.janrupf.thunderwasm.types.ValueType;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashSet;
import java.util.Set;

public class SpectestEnvironment {
    private final MethodHandles.Lookup lookup;

    private final I32Global globalI32;
    private final I64Global globalI64;
    private final F32Global globalF32;
    private final F64Global globalF64;

    private final LinkedFunction print;
    private final LinkedFunction printI32;
    private final LinkedFunction printI64;
    private final LinkedFunction printF32;
    private final LinkedFunction printF64;
    private final LinkedFunction printI32F32;
    private final LinkedFunction printF64F64;

    private final LinkedMemory memory;
    private final LinkedTable<LinkedFunction> table;

    private final Set<String> validImportNames;

    public SpectestEnvironment() {
        this.lookup = MethodHandles.lookup();

        this.globalI32 = new I32Global();
        this.globalI64 = new I64Global();
        this.globalF32 = new F32Global();
        this.globalF64 = new F64Global();

        this.print = lookup("print");
        this.printI32 = lookup("printI32", int.class);
        this.printI64 = lookup("printI64", long.class);
        this.printF32 = lookup("printF32", float.class);
        this.printF64 = lookup("printF64", double.class);
        this.printI32F32 = lookup("printI32F32", int.class, float.class);
        this.printF64F64 = lookup("printF64F64", double.class, double.class);

        this.memory = new LinkedMemory.Simple(new Limits(1, 2));
        this.table = new Table<>(10, 20);

        this.validImportNames = new HashSet<>();
        validImportNames.add("print");
        validImportNames.add("print_i32");
        validImportNames.add("print_i64");
        validImportNames.add("print_f32");
        validImportNames.add("print_f64");
        validImportNames.add("print_i32_f32");
        validImportNames.add("print_f64_f64");
        validImportNames.add("global_i32");
        validImportNames.add("global_i64");
        validImportNames.add("global_f32");
        validImportNames.add("global_f64");
        validImportNames.add("memory");
        validImportNames.add("table");
    }

    private LinkedFunction lookup(String name, Class<?>... argumentTypes) {
        try {
            return LinkedFunction.Simple.inferFromMethodHandle(lookup.findVirtual(
                    SpectestEnvironment.class,
                    name,
                    MethodType.methodType(void.class, argumentTypes)
            ).bindTo(this));
        } catch (Exception e) {
            throw new RuntimeException("Failed to look up spectest implementation, this shouldn't happen", e);
        }
    }

    public LinkedGlobalBase getGlobal(String name, ValueType type, boolean readOnly) throws WastEnvironmentLinker.LinkageFailedException {
        LinkedGlobalBase selected;
        switch (name) {
            case "global_i32":
                requireGlobalType(type, NumberType.I32);
                selected = this.globalI32;
                break;

            case "global_i64":
                requireGlobalType(type, NumberType.I64);
                selected = this.globalI64;
                break;

            case "global_f32":
                requireGlobalType(type, NumberType.F32);
                selected = this.globalF32;
                break;

            case "global_f64":
                requireGlobalType(type, NumberType.F64);
                selected = this.globalF64;
                break;

            default:
                if (!validImportNames.contains(name)) {
                    throw new WastEnvironmentLinker.LinkageFailedException("unknown import");
                }

                throw new WastEnvironmentLinker.LinkageFailedException("incompatible import type");
        }

        if (!readOnly) {
            throw new WastEnvironmentLinker.LinkageFailedException("incompatible import type");
        }

        return selected;
    }

    private void requireGlobalType(ValueType actual, ValueType required) throws WastEnvironmentLinker.LinkageFailedException {
        if (!actual.equals(required)) {
            throw new WastEnvironmentLinker.LinkageFailedException("incompatible import type");
        }
    }

    public LinkedFunction getFunction(String name, FunctionType type) throws WastEnvironmentLinker.LinkageFailedException {
        LinkedFunction selected;
        switch (name) {
            case "print":
                selected = this.print;
                break;

            case "print_i32":
                selected = this.printI32;
                break;

            case "print_i64":
                selected = this.printI64;
                break;

            case "print_f32":
                selected = this.printF32;
                break;

            case "print_f64":
                selected = this.printF64;
                break;

            case "print_i32_f32":
                selected = this.printI32F32;
                break;

            case "print_f64_f64":
                selected = this.printF64F64;
                break;

            default:
                if (!validImportNames.contains(name)) {
                    throw new WastEnvironmentLinker.LinkageFailedException("unknown import");
                }

                throw new WastEnvironmentLinker.LinkageFailedException("incompatible import type");
        }

        if (!type.getInputs().asFlatList().equals(selected.getArguments())) {
            throw new WastEnvironmentLinker.LinkageFailedException("incompatible import type");
        }

        if (!type.getOutputs().asFlatList().equals(selected.getReturnTypes())) {
            throw new WastEnvironmentLinker.LinkageFailedException("incompatible import type");
        }

        return selected;
    }

    private void print() {}

    private void printI32(int value) {
        System.out.println("[print_i32]: " + value);
    }

    private void printI64(long value) {
        System.out.println("[print_i64]: " + value);
    }

    private void printF32(float value) {
        System.out.println("[print_f32]: " + value);
    }

    private void printF64(double value) {
        System.out.println("[print_f64]: " + value);
    }

    private void printI32F32(int a, float b) {
        System.out.println("[print_i32_f32]: " + a + " " + b);
    }

    private void printF64F64(double a, double b) {
        System.out.println("[print_f64_f64]: " + a + " " + b);
    }

    public LinkedMemory getMemory(String name) throws WastEnvironmentLinker.LinkageFailedException {
        if (!name.equals("memory")) {
            if (!validImportNames.contains(name)) {
                throw new WastEnvironmentLinker.LinkageFailedException("unknown import");
            }

            throw new WastEnvironmentLinker.LinkageFailedException("incompatible import type");
        }

        return memory;
    }

    public LinkedTable<?> getTable(String name, ValueType type) throws WastEnvironmentLinker.LinkageFailedException {
        if (!name.equals("table")) {
            if (!validImportNames.contains(name)) {
                throw new WastEnvironmentLinker.LinkageFailedException("unknown import");
            }

            throw new WastEnvironmentLinker.LinkageFailedException("incompatible import type");
        }

        if (!type.equals(ReferenceType.FUNCREF)) {
            throw new WastEnvironmentLinker.LinkageFailedException("incompatible import type");
        }

        return table;
    }

    private static final class I32Global implements LinkedReadOnlyIntGlobal {
        @Override
        public int get() {
            return 666;
        }
    }

    private static final class I64Global implements LinkedReadOnlyLongGlobal {
        @Override
        public long get() {
            return 666;
        }
    }

    private static final class F32Global implements LinkedReadOnlyFloatGlobal {
        @Override
        public float get() {
            return 666.6f;
        }
    }

    private static final class F64Global implements LinkedReadOnlyDoubleGlobal {
        @Override
        public double get() {
            return 666.6;
        }
    }
}
