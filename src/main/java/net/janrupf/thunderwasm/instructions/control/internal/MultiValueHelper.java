package net.janrupf.thunderwasm.instructions.control.internal;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitter;
import net.janrupf.thunderwasm.assembler.emitter.InvokeType;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.runtime.state.MultiValue;

import java.util.List;

public final class MultiValueHelper {
    public static final ObjectType MULTI_VALUE_TYPE = ObjectType.of(MultiValue.class);

    /**
     * Create a multi value which can store the given types.
     *
     * @param emitter      the emitter to use
     * @param storageTypes the types of the values that need to be stored
     * @throws WasmAssemblerException if the code could not be emitted
     */
    public static void emitCreateMultiValue(
            CodeEmitter emitter,
            List<JavaType> storageTypes
    ) throws WasmAssemblerException {
        int intCount = 0;
        int longCount = 0;
        int floatCount = 0;
        int doubleCount = 0;
        int objectCount = 0;

        for (JavaType storageType : storageTypes) {
            if (storageType instanceof PrimitiveType) {
                if (storageType.equals(PrimitiveType.LONG)) {
                    longCount++;
                } else if (storageType.equals(PrimitiveType.FLOAT)) {
                    floatCount++;
                } else if (storageType.equals(PrimitiveType.DOUBLE)) {
                    doubleCount++;
                } else {
                    intCount++;
                }
            } else {
                objectCount++;
            }
        }

        emitCreateMultiValue(emitter, intCount, longCount, floatCount, doubleCount, objectCount);
    }

    /**
     * Create a multi value which can store the given amount of data.
     *
     * @param emitter     the emitter to use
     * @param intCount    the amount of integer space to allocate
     * @param longCount   the amount of long space to allocate
     * @param floatCount  the amount of float space to allocate
     * @param doubleCount the amount of double space to allocate
     * @param objectCount the amount of object space to allocate
     * @throws WasmAssemblerException if the code could not be emitted
     */
    public static void emitCreateMultiValue(
            CodeEmitter emitter,
            int intCount,
            int longCount,
            int floatCount,
            int doubleCount,
            int objectCount
    ) throws WasmAssemblerException {
        emitter.loadConstant(intCount);
        emitter.loadConstant(longCount);
        emitter.loadConstant(floatCount);
        emitter.loadConstant(doubleCount);
        emitter.loadConstant(objectCount);
        emitter.invoke(
                MULTI_VALUE_TYPE,
                "allocate",
                new JavaType[]{PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT},
                MULTI_VALUE_TYPE,
                InvokeType.STATIC,
                false
        );
    }

    /**
     * Saves the given list of locals in that order into a multi
     * value, which is already on top of the stack.
     *
     * @param emitter                the emitter to use
     * @param toSave                 the locals to save
     * @param leaveMultiValueOnStack if true, the multi value is left on the stack
     * @throws WasmAssemblerException if the code could not be emitted
     */
    public static void emitSaveLocals(
            CodeEmitter emitter,
            List<JavaLocal> toSave,
            boolean leaveMultiValueOnStack
    ) throws WasmAssemblerException {
        if (toSave.isEmpty()) {
            if (!leaveMultiValueOnStack) {
                emitter.pop();
            }

            return;
        }

        for (int i = 0; i < toSave.size(); i++) {
            JavaLocal s = toSave.get(i);

            emitter.loadLocal(s);
            if (i != toSave.size() - 1 || leaveMultiValueOnStack) {
                emitter.invoke(
                        MULTI_VALUE_TYPE,
                        "withPut" + multiValueMethodName(s.getType()),
                        new JavaType[]{multiValueMethodType(s.getType())},
                        MULTI_VALUE_TYPE,
                        InvokeType.VIRTUAL,
                        false
                );
            } else {
                emitter.invoke(
                        MULTI_VALUE_TYPE,
                        "put" + multiValueMethodName(s.getType()),
                        new JavaType[]{multiValueMethodType(s.getType())},
                        PrimitiveType.VOID,
                        InvokeType.VIRTUAL,
                        false
                );
            }
        }
    }

    /**
     * Restores the given list of locals in that order from a multi
     * value, which is already on top of the stack.
     *
     * @param emitter                the emitter to use
     * @param toRestore              the locals to restore
     * @param leaveMultiValueOnStack if true, the multi value is left on the stack
     * @throws WasmAssemblerException if the code could not be emitted
     */
    public static void emitRestoreLocals(
            CodeEmitter emitter,
            List<JavaLocal> toRestore,
            boolean leaveMultiValueOnStack
    ) throws WasmAssemblerException {
        if (toRestore.isEmpty()) {
            if (!leaveMultiValueOnStack) {
                emitter.pop();
            }

            return;
        }

        for (int i = toRestore.size() - 1; i >= 0; i--) {
            JavaLocal s = toRestore.get(i);

            if (i != 0 || leaveMultiValueOnStack) {
                emitter.duplicate();
            }

            emitter.invoke(
                    MULTI_VALUE_TYPE,
                    "pop" + multiValueMethodName(s.getType()),
                    new JavaType[0],
                    multiValueMethodType(s.getType()),
                    InvokeType.VIRTUAL,
                    false
            );

            emitter.storeLocal(s);
        }
    }

    /**
     * Emit the code required to save the stack.
     *
     * @param emitter                the emitter to use
     * @param toSave                 the types to save, the last is assumed to be the top of the stack below the multi value
     * @param leaveMultiValueOnStack if true, the multi value is left on the stack
     * @throws WasmAssemblerException if the code could not be emitted
     */
    public static void emitSaveStack(
            CodeEmitter emitter,
            List<JavaType> toSave,
            boolean leaveMultiValueOnStack
    ) throws WasmAssemblerException {
        if (toSave.isEmpty()) {
            if (!leaveMultiValueOnStack) {
                emitter.pop();
            }

            return;
        }

        for (int i = toSave.size() - 1; i >= 0; i--) {
            JavaType type = toSave.get(i);

            if (i != 0 || leaveMultiValueOnStack) {
                emitter.invoke(
                        MULTI_VALUE_TYPE,
                        "staticWithPut" + multiValueMethodName(type),
                        new JavaType[]{multiValueMethodType(type), MULTI_VALUE_TYPE},
                        MULTI_VALUE_TYPE,
                        InvokeType.STATIC,
                        false
                );
            } else {
                emitter.invoke(
                        MULTI_VALUE_TYPE,
                        "staticPut" + multiValueMethodName(type),
                        new JavaType[]{multiValueMethodType(type), MULTI_VALUE_TYPE},
                        PrimitiveType.VOID,
                        InvokeType.STATIC,
                        false
                );
            }
        }
    }

    /**
     * Emit the code required to restore the stack.
     *
     * @param emitter                the emitter to use
     * @param toRestore              the types to save, the last is assumed to be the top of the stack below the multi value
     * @param multiValueLocal        the local the multi value is in, or null, if its on top of the stack
     * @param leaveMultiValueOnStack if true, the multi value is left on the stack
     * @throws WasmAssemblerException if the code could not be emitted
     */
    public static void emitRestoreStack(
            CodeEmitter emitter,
            List<JavaType> toRestore,
            JavaLocal multiValueLocal,
            boolean leaveMultiValueOnStack
    ) throws WasmAssemblerException {
        if (toRestore.isEmpty()) {
            if (!leaveMultiValueOnStack) {
                emitter.pop();
            }

            return;
        } else if (toRestore.size() == 1 && !leaveMultiValueOnStack) {
            // Restore directly, no need to allocate a local
            if (multiValueLocal != null) {
                emitter.loadLocal(multiValueLocal);
            }

            emitter.invoke(
                    MULTI_VALUE_TYPE,
                    "pop" + multiValueMethodName(toRestore.get(0)),
                    new JavaType[0],
                    multiValueMethodType(toRestore.get(0)),
                    InvokeType.VIRTUAL,
                    false
            );
            return;
        }

        JavaLocal selectedLocal;
        if (multiValueLocal == null) {
            selectedLocal = emitter.allocateLocal(MULTI_VALUE_TYPE);
            emitter.storeLocal(selectedLocal);
        } else {
            selectedLocal = multiValueLocal;
        }

        for (JavaType javaType : toRestore) {
            emitter.loadLocal(selectedLocal);
            emitter.invoke(
                    MULTI_VALUE_TYPE,
                    "pop" + multiValueMethodName(javaType),
                    new JavaType[0],
                    multiValueMethodType(javaType),
                    InvokeType.VIRTUAL,
                    false
            );
        }

        if (leaveMultiValueOnStack) {
            emitter.loadLocal(selectedLocal);
        }

        if (multiValueLocal == null) {
            selectedLocal.free();
        }
    }

    /**
     * Emit the code setting a value by index.
     * <p>
     * Expects the multi value to be on top of the stack and below the value
     * to be put into the slot.
     *
     * @param emitter the emitter to use
     * @param type    the type of the value to set
     * @param index   the index of the value in the type specific storage
     * @throws WasmAssemblerException if the code could not be emitted
     */
    public static void emitSetByIndex(CodeEmitter emitter, JavaType type, int index) throws WasmAssemblerException {
        emitter.loadConstant(index);
        emitter.invoke(
                MULTI_VALUE_TYPE,
                "staticSet" + multiValueMethodName(type),
                new JavaType[]{multiValueMethodType(type), MULTI_VALUE_TYPE, PrimitiveType.INT},
                PrimitiveType.VOID,
                InvokeType.STATIC,
                false
        );
    }

    /**
     * Emit the code getting a value by index.
     * <p>
     * Expects the multi value to be on top of the stack.
     *
     * @param emitter the emitter to use
     * @param type    the type of the value to get
     * @param index   the index of the value in the type specific storage
     * @throws WasmAssemblerException if the code could not be emitted
     */
    public static void emitGetByIndex(CodeEmitter emitter, JavaType type, int index) throws WasmAssemblerException {
        emitter.loadConstant(index);
        emitter.invoke(
                MULTI_VALUE_TYPE,
                "get" + multiValueMethodName(type),
                new JavaType[]{PrimitiveType.INT},
                multiValueMethodType(type),
                InvokeType.VIRTUAL,
                false
        );
    }

    private static String multiValueMethodName(JavaType type) {
        if (type instanceof PrimitiveType) {
            if (type.equals(PrimitiveType.LONG)) {
                return "Long";
            } else if (type.equals(PrimitiveType.FLOAT)) {
                return "Float";
            } else if (type.equals(PrimitiveType.DOUBLE)) {
                return "Double";
            }

            return "Int";
        } else {
            return "Object";
        }
    }

    private static JavaType multiValueMethodType(JavaType type) {
        if (type instanceof PrimitiveType) {
            if (type.equals(PrimitiveType.LONG)) {
                return PrimitiveType.LONG;
            } else if (type.equals(PrimitiveType.FLOAT)) {
                return PrimitiveType.FLOAT;
            } else if (type.equals(PrimitiveType.DOUBLE)) {
                return PrimitiveType.DOUBLE;
            }

            return PrimitiveType.INT;
        } else {
            return ObjectType.OBJECT;
        }
    }

    /**
     * Create a new indexed builder.
     *
     * @return the created builder
     */
    public static IndexedBuilder indexedBuilder() {
        return new IndexedBuilder();
    }

    /**
     * Builder for allocating multi value slot indices.
     */
    public static class IndexedBuilder {
        private int intCount;
        private int longCount;
        private int floatCount;
        private int doubleCount;
        private int objectCount;

        private IndexedBuilder() {
            this.intCount = 0;
            this.longCount = 0;
            this.floatCount = 0;
            this.doubleCount = 0;
            this.objectCount = 0;
        }

        /**
         * Allocate a slot index for the given type.
         *
         * @param type the type to allocate a slot for
         * @return the index of the allocated slot
         */
        public int allocate(JavaType type) {
            int idx;

            if (type instanceof PrimitiveType) {
                if (type.equals(PrimitiveType.LONG)) {
                    idx = this.longCount++;
                } else if (type.equals(PrimitiveType.FLOAT)) {
                    idx = this.floatCount++;
                } else if (type.equals(PrimitiveType.DOUBLE)) {
                    idx = this.doubleCount++;
                } else {
                    idx = this.intCount++;
                }
            } else {
                idx = this.objectCount++;
            }

            return idx;
        }

        /**
         * Emit the code that creates the multi value from this builder.
         *
         * @param emitter the emitter to use
         * @throws WasmAssemblerException if the code could not be emitted
         */
        public void emitCreate(CodeEmitter emitter) throws WasmAssemblerException {
            emitCreateMultiValue(emitter, intCount, longCount, floatCount, doubleCount, objectCount);
        }
    }
}
