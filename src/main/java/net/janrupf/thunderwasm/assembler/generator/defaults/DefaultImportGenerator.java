package net.janrupf.thunderwasm.assembler.generator.defaults;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.signature.ConcreteType;
import net.janrupf.thunderwasm.assembler.emitter.signature.SignaturePart;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.assembler.generator.ImportGenerator;
import net.janrupf.thunderwasm.data.Limits;
import net.janrupf.thunderwasm.imports.GlobalImportDescription;
import net.janrupf.thunderwasm.imports.Import;
import net.janrupf.thunderwasm.imports.MemoryImportDescription;
import net.janrupf.thunderwasm.imports.TableImportDescription;
import net.janrupf.thunderwasm.instructions.memory.base.PlainMemory;
import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryLoad;
import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryStore;
import net.janrupf.thunderwasm.runtime.linker.RuntimeLinker;
import net.janrupf.thunderwasm.runtime.linker.global.LinkedGlobal;
import net.janrupf.thunderwasm.runtime.linker.table.LinkedTable;
import net.janrupf.thunderwasm.types.*;

import java.util.HashMap;
import java.util.Map;

public class DefaultImportGenerator implements ImportGenerator {
    private static final ObjectType RUNTIME_LINKER_TYPE = ObjectType.of(RuntimeLinker.class);

    private final Map<String, String> identifierNameCache;
    private final Map<String, DefaultTableGenerator> importedTableGenerators;
    private final Map<String, DefaultMemoryGenerator> importedMemoryGenerators;

    public DefaultImportGenerator() {
        this.identifierNameCache = new HashMap<>();
        this.importedTableGenerators = new HashMap<>();
        this.importedMemoryGenerators = new HashMap<>();
    }

    @Override
    public ObjectType getLinkerType() {
        return RUNTIME_LINKER_TYPE;
    }

    @Override
    public void addImport(Import<?> im, ClassFileEmitter emitter) throws WasmAssemblerException {
        Import<GlobalImportDescription> globalImport = im.tryCast(GlobalImportDescription.class);
        if (globalImport != null) {
            addGlobalImport(globalImport, emitter);
            return;
        }

        Import<TableImportDescription> tableImport = im.tryCast(TableImportDescription.class);
        if (tableImport != null) {
            addTableImport(tableImport, emitter);
            return;
        }
    }

    private void addGlobalImport(Import<GlobalImportDescription> im, ClassFileEmitter emitter)
            throws WasmAssemblerException {
        String fieldName = generateImportFieldName(im);
        ValueType valueType = im.getDescription().getType().getValueType();
        boolean isReadOnly = im.getDescription().getType().getMutability() == GlobalType.Mutability.CONST;

        // Select the field type to use
        DefaultFieldTypeLookup.Selected type = DefaultFieldTypeLookup.GLOBAL_IMPORT.select(
                valueType,
                isReadOnly
        );

        SignaturePart signature = null;
        if (type.isGeneric()) {
            signature = ConcreteType
                    .builder(type.getType())
                    .withTypeArgument(ConcreteType.of((ObjectType) WasmTypeConverter.toJavaType(valueType)))
                    .build();
        }

        emitter.field(
                fieldName,
                Visibility.PRIVATE,
                false,
                true,
                type.getType(),
                signature
        );
    }

    private void addTableImport(Import<TableImportDescription> im, ClassFileEmitter emitter)
            throws WasmAssemblerException {
        tableGeneratorFor(im).addTable(null, im.getDescription().getType(), emitter);
    }

    @Override
    public void emitLinkImport(Import<?> im, CodeEmitContext context) throws WasmAssemblerException {
        Import<GlobalImportDescription> globalImport = im.tryCast(GlobalImportDescription.class);
        if (globalImport != null) {
            emitLinkGlobalImport(globalImport, context);
            return;
        }

        Import<TableImportDescription> tableImport = im.tryCast(TableImportDescription.class);
        if (tableImport != null) {
            emitLinkTableImport(tableImport, context);
            return;
        }

        // For now just pop the linker if we don't handle the import
        context.getFrameState().popOperand(ReferenceType.OBJECT);
        context.getEmitter().pop(RUNTIME_LINKER_TYPE);
    }

    private void emitLinkGlobalImport(Import<GlobalImportDescription> im, CodeEmitContext context)
            throws WasmAssemblerException {
        ValueType type = im.getDescription().getType().getValueType();
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();

        boolean isConst = im.getDescription().getType().getMutability() == GlobalType.Mutability.CONST;

        // Push the arguments for the method invocation, we expect the linker to be on top of the stack already

        frameState.pushOperand(ReferenceType.OBJECT);
        emitter.loadConstant(im.getModule());

        frameState.pushOperand(ReferenceType.OBJECT);
        emitter.loadConstant(im.getName());

        CommonBytecodeGenerator.loadTypeReference(frameState, emitter, type);

        frameState.pushOperand(NumberType.I32);
        emitter.loadConstant(isConst);

        emitter.invoke(
                RUNTIME_LINKER_TYPE,
                "linkGlobal",
                new JavaType[]{
                        ObjectType.of(String.class),
                        ObjectType.of(String.class),
                        ObjectType.of(ValueType.class),
                        PrimitiveType.BOOLEAN
                },
                ObjectType.of(LinkedGlobal.class),
                InvokeType.INTERFACE,
                true
        );

        // TODO: Make it configurable whether the return type is checked
        // DEBUG: Check if the return type is correct
        emitter.checkCast(DefaultFieldTypeLookup.GLOBAL_IMPORT.select(
                type,
                isConst
        ).getType());

        // Pop Arguments from the stack
        frameState.popOperand(NumberType.I32);
        frameState.popOperand(ReferenceType.OBJECT);
        frameState.popOperand(ReferenceType.OBJECT);
        frameState.popOperand(ReferenceType.OBJECT);

        // Don't bother popping the "linker" instance, it has been replaced by another OBJECT reference

        // Set the field to the result of the method invocation
        accessImportField(im, context, true);
    }

    private void emitLinkTableImport(Import<TableImportDescription> im, CodeEmitContext context)
            throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();
        ReferenceType type = im.getDescription().getType().getElementType();

        // Push the arguments for the method invocation, we expect the linker to be on top of the stack already

        frameState.pushOperand(ReferenceType.OBJECT);
        emitter.loadConstant(im.getModule());

        frameState.pushOperand(ReferenceType.OBJECT);
        emitter.loadConstant(im.getName());

        CommonBytecodeGenerator.loadTypeReference(frameState, emitter, type);
        CommonBytecodeGenerator.loadLimits(frameState, emitter, im.getDescription().getType().getLimits());

        emitter.invoke(
                RUNTIME_LINKER_TYPE,
                "linkTable",
                new JavaType[]{
                        ObjectType.of(String.class),
                        ObjectType.of(String.class),
                        ObjectType.of(ReferenceType.class),
                        ObjectType.of(Limits.class),
                },
                ObjectType.of(LinkedTable.class),
                InvokeType.INTERFACE,
                true
        );

        // Pop Arguments from the stack
        frameState.popOperand(ReferenceType.OBJECT);
        frameState.popOperand(ReferenceType.OBJECT);
        frameState.popOperand(ReferenceType.OBJECT);
        frameState.popOperand(ReferenceType.OBJECT);

        // Don't bother popping the "linker" instance, it has been replaced by another OBJECT reference

        // Set the field to the result of the method invocation
        frameState.pushOperand(ReferenceType.OBJECT);
        emitter.loadThis();
        emitter.op(Op.SWAP);
        emitter.accessField(
                emitter.getOwner(),
                generateImportFieldName(im),
                DefaultTableGenerator.LINKED_TABLE_TYPE,
                false,
                true
        );

        frameState.popOperand(ReferenceType.OBJECT);
        frameState.popOperand(ReferenceType.OBJECT);
    }

    @Override
    public void emitGetGlobal(Import<GlobalImportDescription> im, CodeEmitContext context) throws WasmAssemblerException {
        // Load the linked global
        accessImportField(im, context, false);

        DefaultFieldTypeLookup.Selected selectedStorage = DefaultFieldTypeLookup.GLOBAL_IMPORT.select(
                im.getDescription().getType().getValueType(),
                false
        );

        JavaType argType;
        if (selectedStorage.isGeneric()) {
            argType = ObjectType.OBJECT;
        } else {
            argType = WasmTypeConverter.toJavaType(im.getDescription().getType().getValueType());
        }

        // We now have the global on the stack
        context.getEmitter().invoke(
                selectedStorage.getType(),
                "get",
                new JavaType[0],
                argType,
                InvokeType.INTERFACE,
                true
        );

        if (selectedStorage.isGeneric()) {
            context.getEmitter().checkCast(
                    (ObjectType) WasmTypeConverter.toJavaType(im.getDescription().getType().getValueType())
            );
        }

        context.getFrameState().popOperand(ReferenceType.OBJECT);
        context.getFrameState().pushOperand(im.getDescription().getType().getValueType());
    }

    @Override
    public void emitSetGlobal(Import<GlobalImportDescription> im, CodeEmitContext context) throws WasmAssemblerException {
        // Load the linked global
        accessImportField(im, context, false);

        // We now have the global on the stack, swap it with the value
        CommonBytecodeGenerator.swap(context.getFrameState(), context.getEmitter());

        DefaultFieldTypeLookup.Selected selectedStorage = DefaultFieldTypeLookup.GLOBAL_IMPORT.select(
                im.getDescription().getType().getValueType(),
                false
        );

        JavaType argType;
        if (selectedStorage.isGeneric()) {
            argType = ObjectType.OBJECT;
        } else {
            argType = WasmTypeConverter.toJavaType(im.getDescription().getType().getValueType());
        }

        // We now have the value on top of the stack, call the set method
        context.getEmitter().invoke(
                selectedStorage.getType(),
                "set",
                new JavaType[]{argType},
                PrimitiveType.VOID,
                InvokeType.INTERFACE,
                true
        );

        // Pop the global and the value from the stack
        context.getFrameState().popOperand(im.getDescription().getType().getValueType());
        context.getFrameState().popOperand(ReferenceType.OBJECT);
    }

    @Override
    public void emitTableGet(Import<TableImportDescription> im, CodeEmitContext context) throws WasmAssemblerException {
        tableGeneratorFor(im).emitTableGet(null, im.getDescription().getType(), context);
    }

    @Override
    public void emitTableSet(Import<TableImportDescription> im, CodeEmitContext context) throws WasmAssemblerException {
        tableGeneratorFor(im).emitTableSet(null, im.getDescription().getType(), context);
    }

    @Override
    public void emitTableGrow(Import<TableImportDescription> im, CodeEmitContext context) throws WasmAssemblerException {
        tableGeneratorFor(im).emitTableGrow(null, im.getDescription().getType(), context);
    }

    @Override
    public void emitTableSize(Import<TableImportDescription> im, CodeEmitContext context) throws WasmAssemblerException {
        tableGeneratorFor(im).emitTableSize(null, im.getDescription().getType(), context);
    }

    @Override
    public void emitTableFill(Import<TableImportDescription> im, CodeEmitContext context) throws WasmAssemblerException {
        tableGeneratorFor(im).emitTableFill(null, im.getDescription().getType(), context);
    }

    @Override
    public void emitLoadTableReference(Import<TableImportDescription> im, CodeEmitContext context) throws WasmAssemblerException {
        tableGeneratorFor(im).emitLoadTableReference(null, context);
    }

    @Override
    public ObjectType getTableType(Import<TableImportDescription> im) {
        return tableGeneratorFor(im).getTableType(null);
    }

    @Override
    public void emitMemoryStore(Import<MemoryImportDescription> im, NumberType numberType, PlainMemory.Memarg memarg, PlainMemoryStore.StoreType storeType, CodeEmitContext context) throws WasmAssemblerException {
        memoryGeneratorFor(im).emitStore(
                null,
                im.getDescription().getType(),
                numberType,
                memarg,
                storeType,
                context
        );
    }

    @Override
    public void emitMemoryLoad(Import<MemoryImportDescription> im, NumberType numberType, PlainMemory.Memarg memarg, PlainMemoryLoad.LoadType loadType, CodeEmitContext context) throws WasmAssemblerException {
        memoryGeneratorFor(im).emitLoad(
                null,
                im.getDescription().getType(),
                numberType,
                memarg,
                loadType,
                context
        );
    }

    @Override
    public void emitLoadMemoryReference(Import<MemoryImportDescription> im, CodeEmitContext context) throws WasmAssemblerException {
        memoryGeneratorFor(im).emitLoadMemoryReference(null, context);
    }

    @Override
    public ObjectType getMemoryType(Import<MemoryImportDescription> im) {
        return memoryGeneratorFor(im).getMemoryType(null);
    }

    /**
     * Generates a unique field name for an import.
     *
     * @param im the import to generate a field name for
     * @return the generated field name
     */
    private String generateImportFieldName(Import<?> im) {
        return "import$" + makeJavaIdentifier(im.getModule()) + "$" + makeJavaIdentifier(im.getName());
    }

    /**
     * Generate the code for accessing an import field.
     *
     * @param im      the import to access
     * @param context the context to use
     * @param isSet   whether the field is being set
     * @throws WasmAssemblerException if an error occurs during assembly
     */
    private void accessImportField(
            Import<GlobalImportDescription> im,
            CodeEmitContext context,
            boolean isSet
    ) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();

        CommonBytecodeGenerator.loadThisBelow(frameState, emitter, isSet ? 1 : 0);
        emitter.accessField(
                emitter.getOwner(),
                generateImportFieldName(im),
                DefaultFieldTypeLookup.GLOBAL_IMPORT.select(
                        im.getDescription().getType().getValueType(),
                        im.getDescription().getType().getMutability() == GlobalType.Mutability.CONST
                ).getType(),
                false,
                isSet
        );
        frameState.popOperand(ReferenceType.OBJECT);

        if (isSet) {
            frameState.popOperand(ReferenceType.OBJECT);
        } else {
            frameState.pushOperand(ReferenceType.OBJECT);
        }
    }

    /**
     * Converts a string to a valid Java identifier while maintaining uniqueness.
     *
     * @param name the name to convert
     * @return the converted name
     */
    private String makeJavaIdentifier(String name) {
        String cached = identifierNameCache.get(name);
        if (cached != null) {
            return cached;
        }

        boolean needsStart = true;

        StringBuilder b = new StringBuilder();

        for (char c : name.toCharArray()) {
            boolean isValid;

            if (needsStart) {
                isValid = Character.isJavaIdentifierStart(c);
                needsStart = false;
            } else {
                isValid = Character.isJavaIdentifierPart(c);
            }

            if (isValid && c != '$') {
                b.append(c);
            } else {
                b.append('$');
                b.append((int) c);
            }
        }

        identifierNameCache.put(name, b.toString());
        return b.toString();
    }

    private DefaultTableGenerator tableGeneratorFor(Import<TableImportDescription> im) {
        String fieldName = generateImportFieldName(im);
        return importedTableGenerators.computeIfAbsent(fieldName, (key) -> new DefaultTableGenerator(
                DefaultTableGenerator.LINKED_TABLE_TYPE,
                key
        ));
    }

    private DefaultMemoryGenerator memoryGeneratorFor(Import<MemoryImportDescription> im) {
        String fieldName = generateImportFieldName(im);
        return importedMemoryGenerators.computeIfAbsent(fieldName, DefaultMemoryGenerator::new);
    }
}
