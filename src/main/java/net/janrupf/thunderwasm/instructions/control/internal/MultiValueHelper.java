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
     * @param emitter the emitter to use
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

        emitter.loadConstant(intCount);
        emitter.loadConstant(longCount);
        emitter.loadConstant(floatCount);
        emitter.loadConstant(doubleCount);
        emitter.loadConstant(objectCount);
        emitter.invoke(
                MULTI_VALUE_TYPE,
                "allocate",
                new JavaType[] { PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT },
                MULTI_VALUE_TYPE,
                InvokeType.STATIC,
                false
        );
    }

    /**
     * Saves the given list of locals in that order into a multi
     * value, which is already on top of the stack.
     *
     * @param emitter the emitter to use
     * @param toSave the locals to save
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
                        new JavaType[]{ multiValueMethodType(s.getType()) },
                        MULTI_VALUE_TYPE,
                        InvokeType.VIRTUAL,
                        false
                );
            } else {
                emitter.invoke(
                        MULTI_VALUE_TYPE,
                        "put" + multiValueMethodName(s.getType()),
                        new JavaType[]{ multiValueMethodType(s.getType()) },
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
     * @param emitter the emitter to use
     * @param toRestore the locals to restore
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
     * @param emitter the emitter to use
     * @param toSave the types to save, the last is assumed to be the top of the stack below the multi value
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
                        new JavaType[] { multiValueMethodType(type), MULTI_VALUE_TYPE },
                        MULTI_VALUE_TYPE,
                        InvokeType.STATIC,
                        false
                );
            } else {
                emitter.invoke(
                        MULTI_VALUE_TYPE,
                        "staticPut" + multiValueMethodName(type),
                        new JavaType[] { multiValueMethodType(type), MULTI_VALUE_TYPE },
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
     * @param emitter the emitter to use
     * @param toRestore the types to save, the last is assumed to be the top of the stack below the multi value
     * @param multiValueLocal the local the multi value is in, or null, if its on top of the stack
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
}
