package net.janrupf.thunderwasm.assembler.generator.defaults;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.signature.ConcreteType;
import net.janrupf.thunderwasm.assembler.emitter.types.ArrayType;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.assembler.generator.TableGenerator;
import net.janrupf.thunderwasm.data.Limits;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.module.section.segment.ElementSegment;
import net.janrupf.thunderwasm.runtime.ElementReference;
import net.janrupf.thunderwasm.runtime.ExternReference;
import net.janrupf.thunderwasm.runtime.FunctionReference;
import net.janrupf.thunderwasm.runtime.Table;
import net.janrupf.thunderwasm.runtime.linker.table.LinkedTable;
import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.ReferenceType;
import net.janrupf.thunderwasm.types.TableType;

public class DefaultTableGenerator implements TableGenerator {
    public static final ObjectType CONCRETE_TABLE_TYPE = ObjectType.of(Table.class);
    public static final ObjectType LINKED_TABLE_TYPE = ObjectType.of(LinkedTable.class);

    private final ObjectType tableType;
    private final String fieldName;

    public DefaultTableGenerator() {
        this(ObjectType.of(Table.class));
    }

    public DefaultTableGenerator(ObjectType tableType) {
        this(tableType, null);
    }

    public DefaultTableGenerator(ObjectType tableType, String fieldName) {
        this.tableType = tableType;
        this.fieldName = fieldName;

        if (!tableType.equals(CONCRETE_TABLE_TYPE) && !tableType.equals(LINKED_TABLE_TYPE)) {
            throw new IllegalArgumentException("Unsupported table type: " + tableType);
        }
    }

    @Override
    public void addTable(LargeArrayIndex i, TableType type, ClassFileEmitter emitter) throws WasmAssemblerException {
        ObjectType genericTableType = objectTypeFor(type.getElementType());

        emitter.field(
                generateTableFieldName(i),
                Visibility.PRIVATE,
                false,
                true,
                tableType,
                ConcreteType.builder(tableType)
                        .withTypeArgument(ConcreteType.of(genericTableType))
                        .build()
        );
    }

    @Override
    public void addElementSegment(LargeArrayIndex i, ElementSegment segment, ClassFileEmitter emitter)
            throws WasmAssemblerException {
        ObjectType arrayElementType = objectTypeFor(segment.getType());

        emitter.field(
                generateElementSegmentFieldName(i),
                Visibility.PRIVATE,
                false,
                false,
                new ArrayType(arrayElementType),
                null
        );
    }

    @Override
    public void emitTableConstructor(LargeArrayIndex i, TableType type, CodeEmitContext context)
            throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();
        Limits limits = type.getLimits();

        frameState.pushOperand(ReferenceType.OBJECT);
        frameState.pushOperand(ReferenceType.OBJECT);
        frameState.pushOperand(NumberType.I32);
        frameState.pushOperand(NumberType.I32);

        emitter.doNew(tableType);
        emitter.duplicate(tableType);
        emitter.loadConstant(limits.getMin());

        if (limits.getMax() == null) {
            emitter.loadConstant(-1);
        } else {
            emitter.loadConstant(limits.getMax());
        }

        emitter.invoke(
                tableType,
                "<init>",
                new JavaType[]{PrimitiveType.INT, PrimitiveType.INT},
                PrimitiveType.VOID,
                InvokeType.SPECIAL,
                false
        );

        frameState.popOperand(NumberType.I32);
        frameState.popOperand(NumberType.I32);
        frameState.popOperand(ReferenceType.OBJECT);

        frameState.pushOperand(ReferenceType.OBJECT);
        emitter.loadThis();
        emitter.op(Op.SWAP);
        emitter.accessField(
                context.getEmitter().getOwner(),
                generateTableFieldName(i),
                tableType,
                false,
                true
        );
    }

    @Override
    public void emitElementSegmentConstructor(
            LargeArrayIndex i,
            ElementSegment segment,
            Object[] initValues,
            CodeEmitContext context
    ) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();
        WasmFrameState frameState = context.getFrameState();

        ObjectType arrayElementType = objectTypeFor(segment.getType());
        ArrayType arrayType = new ArrayType(arrayElementType);

        frameState.pushOperand(ReferenceType.OBJECT);
        emitter.loadConstant(initValues.length);
        emitter.doNew(arrayType);


        for (int j = 0; j < initValues.length; j++) {
            frameState.pushOperand(ReferenceType.OBJECT);
            frameState.pushOperand(NumberType.I32);

            emitter.duplicate(arrayType);
            emitter.loadConstant(j);

            CommonBytecodeGenerator.loadConstant(emitter, frameState, segment.getType(), initValues[j]);
            emitter.storeArrayElement(arrayType);

            frameState.popOperand(segment.getType());
            frameState.popOperand(NumberType.I32);
            frameState.popOperand(ReferenceType.OBJECT);
        }

        CommonBytecodeGenerator.loadThisBelow(context.getFrameState(), context.getEmitter(), 1);
        emitter.accessField(
                context.getEmitter().getOwner(),
                generateElementSegmentFieldName(i),
                arrayType,
                false,
                true
        );

        frameState.popOperand(ReferenceType.OBJECT);
        frameState.popOperand(ReferenceType.OBJECT);
    }

    @Override
    public void emitTableGet(LargeArrayIndex i, TableType type, CodeEmitContext context) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();
        WasmFrameState frameState = context.getFrameState();

        CommonBytecodeGenerator.loadBelow(
                context.getFrameState(),
                context.getEmitter(),
                1,
                ReferenceType.OBJECT,
                () -> emitLoadTableReferenceInternal(i, context)
        );

        emitter.invoke(
                tableType,
                "get",
                new JavaType[]{PrimitiveType.INT},
                ObjectType.of(ElementReference.class),
                InvokeType.INTERFACE,
                true
        );

        // Pop the index and this
        frameState.popOperand(NumberType.I32);
        frameState.popOperand(ReferenceType.OBJECT);

        // Push the element reference
        frameState.pushOperand(type.getElementType());
        emitter.checkCast((ObjectType) WasmTypeConverter.toJavaType(type.getElementType()));
    }

    @Override
    public void emitTableSet(LargeArrayIndex i, TableType type, CodeEmitContext context) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();
        WasmFrameState frameState = context.getFrameState();

        CommonBytecodeGenerator.loadBelow(
                context.getFrameState(),
                context.getEmitter(),
                2,
                ReferenceType.OBJECT,
                () -> emitLoadTableReferenceInternal(i, context)
        );

        emitter.invoke(
                tableType,
                "set",
                new JavaType[]{PrimitiveType.INT, ObjectType.of(ElementReference.class)},
                PrimitiveType.VOID,
                InvokeType.INTERFACE,
                true
        );

        frameState.popOperand(type.getElementType());
        frameState.popOperand(NumberType.I32);
        frameState.popOperand(ReferenceType.OBJECT);
    }

    @Override
    public void emitTableCopy(TableType sourceType, TableType targetType, CodeEmitContext context)
            throws WasmAssemblerException {
        if (!sourceType.getElementType().equals(targetType.getElementType())) {
            throw new WasmAssemblerException("Cannot copy tables with different element types");
        }

        // This method is somewhat special, because it always operates on the LinkedTable interface
        // and the required references are already on top of the stack
        CodeEmitter emitter = context.getEmitter();
        WasmFrameState frameState = context.getFrameState();

        emitter.invoke(
                LINKED_TABLE_TYPE,
                "copy",
                new JavaType[]{PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, LINKED_TABLE_TYPE},
                PrimitiveType.VOID,
                InvokeType.INTERFACE,
                true
        );

        // Clean up the stack
        frameState.popOperand(ReferenceType.OBJECT);
        frameState.popOperand(NumberType.I32);
        frameState.popOperand(NumberType.I32);
        frameState.popOperand(NumberType.I32);
        frameState.popOperand(ReferenceType.OBJECT);
    }

    @Override
    public void emitTableGrow(LargeArrayIndex i, TableType type, CodeEmitContext context) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();
        WasmFrameState frameState = context.getFrameState();

        CommonBytecodeGenerator.loadBelow(
                context.getFrameState(),
                context.getEmitter(),
                2,
                ReferenceType.OBJECT,
                () -> emitLoadTableReferenceInternal(i, context)
        );

        emitter.invoke(
                tableType,
                "grow",
                new JavaType[]{ObjectType.of(ElementReference.class), PrimitiveType.INT},
                PrimitiveType.INT,
                InvokeType.INTERFACE,
                true
        );

        frameState.popOperand(NumberType.I32);
        frameState.popOperand(type.getElementType());
        frameState.popOperand(ReferenceType.OBJECT);
        frameState.pushOperand(NumberType.I32);
    }

    @Override
    public void emitTableSize(LargeArrayIndex i, TableType type, CodeEmitContext context) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();
        WasmFrameState frameState = context.getFrameState();

        CommonBytecodeGenerator.loadBelow(
                context.getFrameState(),
                context.getEmitter(),
                0,
                ReferenceType.OBJECT,
                () -> emitLoadTableReferenceInternal(i, context)
        );

        emitter.invoke(
                tableType,
                "size",
                new JavaType[0],
                PrimitiveType.INT,
                InvokeType.INTERFACE,
                true
        );

        frameState.popOperand(ReferenceType.OBJECT);
        frameState.pushOperand(NumberType.I32);
    }

    @Override
    public void emitTableFill(LargeArrayIndex i, TableType type, CodeEmitContext context) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();

        CommonBytecodeGenerator.loadBelow(
                frameState,
                emitter,
                3,
                ReferenceType.OBJECT,
                () -> emitLoadTableReferenceInternal(i, context)
        );

        emitter.invoke(
                tableType,
                "fill",
                new JavaType[]{PrimitiveType.INT, ObjectType.of(ElementReference.class), PrimitiveType.INT},
                PrimitiveType.VOID,
                InvokeType.INTERFACE,
                true
        );

        frameState.popOperand(NumberType.I32);
        frameState.popOperand(type.getElementType());
        frameState.popOperand(NumberType.I32);
        frameState.popOperand(ReferenceType.OBJECT);
    }

    @Override
    public void emitTableInit(
            TableType tableType,
            LargeArrayIndex elementIndex,
            ElementSegment segment,
            CodeEmitContext context
    ) throws WasmAssemblerException {
        if (!tableType.getElementType().equals(segment.getType())) {
            throw new WasmAssemblerException("Cannot initialize table with element segment of different type");
        }

        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();

        frameState.pushOperand(ReferenceType.OBJECT);
        emitter.loadThis();
        emitter.accessField(
                context.getEmitter().getOwner(),
                generateElementSegmentFieldName(elementIndex),
                new ArrayType(objectTypeFor(segment.getType())),
                false,
                false
        );

        emitter.invoke(
                this.tableType,
                "init",
                new JavaType[]{PrimitiveType.INT, PrimitiveType.INT, PrimitiveType.INT, new ArrayType(ObjectType.of(ElementReference.class))},
                PrimitiveType.VOID,
                InvokeType.INTERFACE,
                true
        );

        frameState.popOperand(ReferenceType.OBJECT);
        frameState.popOperand(NumberType.I32);
        frameState.popOperand(NumberType.I32);
        frameState.popOperand(NumberType.I32);
        frameState.popOperand(ReferenceType.OBJECT);
    }

    @Override
    public void emitLoadTableReference(LargeArrayIndex i, CodeEmitContext context) throws WasmAssemblerException {
        emitLoadTableReferenceInternal(i, context);
        context.getFrameState().pushOperand(ReferenceType.OBJECT);
    }

    @Override
    public void emitLoadElement(LargeArrayIndex i, ElementSegment segment, CodeEmitContext context) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();
        WasmFrameState frameState = context.getFrameState();

        frameState.pushOperand(ReferenceType.OBJECT);
        emitter.loadThis();
        emitter.accessField(
                context.getEmitter().getOwner(),
                generateElementSegmentFieldName(i),
                new ArrayType(objectTypeFor(segment.getType())),
                false,
                false
        );
        frameState.popOperand(ReferenceType.OBJECT);
        frameState.popOperand(NumberType.I32);
        emitter.op(Op.SWAP);
        frameState.pushOperand(ReferenceType.OBJECT);
        frameState.pushOperand(NumberType.I32);

        emitter.loadArrayElement(new ArrayType(objectTypeFor(segment.getType())));

        frameState.popOperand(NumberType.I32);
        frameState.popOperand(ReferenceType.OBJECT);

        frameState.pushOperand(segment.getType());
    }

    /**
     * Emit the code for loading the table reference.
     * <p>
     * This method does not modify the frame state!
     *
     * @param i       the index of the table
     * @param context the context to use
     * @throws WasmAssemblerException if an error occurs
     */
    private void emitLoadTableReferenceInternal(LargeArrayIndex i, CodeEmitContext context) throws WasmAssemblerException {
        context.getEmitter().loadThis();
        context.getEmitter().accessField(
                context.getEmitter().getOwner(),
                generateTableFieldName(i),
                tableType,
                false,
                false
        );
    }

    @Override
    public ObjectType getTableType(LargeArrayIndex i) {
        return tableType;
    }

    @Override
    public boolean canEmitCopyFor(ObjectType from, ObjectType to) {
        return (
                from.equals(CONCRETE_TABLE_TYPE) || from.equals(LINKED_TABLE_TYPE)
        ) && (
                to.equals(CONCRETE_TABLE_TYPE) || to.equals(LINKED_TABLE_TYPE)
        );
    }

    @Override
    public boolean canEmitInitFor(ObjectType to) {
        return to.equals(CONCRETE_TABLE_TYPE) || to.equals(LINKED_TABLE_TYPE);
    }

    protected String generateTableFieldName(LargeArrayIndex i) {
        if (i == null && fieldName == null) {
            throw new IllegalArgumentException("If no index is given, the field name needs to be known in advance");
        }

        if (fieldName != null) {
            return fieldName;
        }

        return "table_" + i;
    }

    protected String generateElementSegmentFieldName(LargeArrayIndex i) {
        if (fieldName != null) {
            throw new IllegalStateException("Field name is not supported for element segments");
        }

        return "element_segment_" + i;
    }

    protected ObjectType objectTypeFor(ReferenceType type) throws WasmAssemblerException {
        if (type.equals(ReferenceType.EXTERNREF)) {
            return ObjectType.of(ExternReference.class);
        } else if (type.equals(ReferenceType.FUNCREF)) {
            return ObjectType.of(FunctionReference.class);
        } else {
            throw new WasmAssemblerException("Unsupported reference type: " + type);
        }
    }
}
