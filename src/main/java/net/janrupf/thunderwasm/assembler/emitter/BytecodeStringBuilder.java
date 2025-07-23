package net.janrupf.thunderwasm.assembler.emitter;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;

/**
 * Helper for generating string concatenation bytecode.
 */
public class BytecodeStringBuilder {
    private static final ObjectType STRING_BUILDER_TYPE = ObjectType.of(StringBuilder.class);

    private final CodeEmitter emitter;

    public BytecodeStringBuilder(CodeEmitter emitter) throws WasmAssemblerException {
        this(emitter, -1);
    }

    public BytecodeStringBuilder(CodeEmitter emitter, int initialCapacity) throws WasmAssemblerException {
        this.emitter = emitter;
        emitter.doNew(STRING_BUILDER_TYPE);
        emitter.duplicate();

        if (initialCapacity >= 0) {
            emitter.loadConstant(initialCapacity);
            emitter.invoke(
                    STRING_BUILDER_TYPE,
                    "<init>",
                    new JavaType[] { PrimitiveType.INT },
                    PrimitiveType.VOID,
                    InvokeType.SPECIAL,
                    false
            );
        } else {
            emitter.invoke(
                    STRING_BUILDER_TYPE,
                    "<init>",
                    new JavaType[0],
                    PrimitiveType.VOID,
                    InvokeType.SPECIAL,
                    false
            );
        }
    }

    /**
     * Append a string value.
     *
     * @param value the string value to append
     * @throws WasmAssemblerException if an error occurs during bytecode generation
     */
    public void append(String value) throws WasmAssemblerException {
        emitter.loadConstant(value);
        append(ObjectType.of(String.class));
    }

    /**
     * Append a Java type value.
     *
     * @param type the Java type to append
     * @throws WasmAssemblerException if an error occurs during bytecode generation
     */
    public void append(JavaType type) throws WasmAssemblerException {
        emitter.invoke(
                STRING_BUILDER_TYPE,
                "append",
                new JavaType[] { type },
                STRING_BUILDER_TYPE,
                InvokeType.VIRTUAL,
                false
        );
    }

    /**
     * Invokes the `toString` method on the StringBuilder instance
     *
     * @throws WasmAssemblerException if an error occurs during bytecode generation
     */
    public void finish() throws WasmAssemblerException {
        emitter.invoke(
                STRING_BUILDER_TYPE,
                "toString",
                new JavaType[0],
                ObjectType.of(String.class),
                InvokeType.VIRTUAL,
                false
        );
    }
}
