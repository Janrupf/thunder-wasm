package net.janrupf.thunderwasm.assembler.generator.defaults;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.emitter.*;
import net.janrupf.thunderwasm.assembler.emitter.frame.JavaLocal;
import net.janrupf.thunderwasm.assembler.emitter.signature.ConcreteType;
import net.janrupf.thunderwasm.assembler.emitter.signature.SignaturePart;
import net.janrupf.thunderwasm.assembler.emitter.types.JavaType;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.assembler.emitter.types.PrimitiveType;
import net.janrupf.thunderwasm.assembler.generator.ImportGenerator;
import net.janrupf.thunderwasm.data.Limits;
import net.janrupf.thunderwasm.imports.*;
import net.janrupf.thunderwasm.instructions.memory.base.PlainMemory;
import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryLoad;
import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryStore;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.module.section.segment.DataSegment;
import net.janrupf.thunderwasm.runtime.FunctionReference;
import net.janrupf.thunderwasm.runtime.linker.RuntimeLinker;
import net.janrupf.thunderwasm.runtime.linker.function.LinkedFunction;
import net.janrupf.thunderwasm.runtime.linker.global.LinkedGlobal;
import net.janrupf.thunderwasm.runtime.linker.memory.LinkedMemory;
import net.janrupf.thunderwasm.runtime.linker.table.LinkedTable;
import net.janrupf.thunderwasm.types.*;

import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.Map;

public class DefaultImportGenerator implements ImportGenerator {
    private static final ObjectType RUNTIME_LINKER_TYPE = ObjectType.of(RuntimeLinker.class);
    private static final ObjectType LINKED_MEMORY_TYPE = ObjectType.of(LinkedMemory.class);
    private static final ObjectType LINKED_FUNCTION_TYPE = ObjectType.of(LinkedFunction.class);

    private final Map<String, String> identifierNameCache;
    private final Map<String, DefaultTableGenerator> importedTableGenerators;
    private final Map<String, DefaultMemoryGenerator> importedMemoryGenerators;
    private final DefaultFunctionGenerator functionGenerator;

    public DefaultImportGenerator() {
        this.identifierNameCache = new HashMap<>();
        this.importedTableGenerators = new HashMap<>();
        this.importedMemoryGenerators = new HashMap<>();
        this.functionGenerator = new DefaultFunctionGenerator();
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

        Import<MemoryImportDescription> memoryImport = im.tryCast(MemoryImportDescription.class);
        if (memoryImport != null) {
            addMemoryImport(memoryImport, emitter);
            return;
        }

        Import<TypeImportDescription> typeImport = im.tryCast(TypeImportDescription.class);
        if (typeImport != null) {
            addTypeImport(typeImport, emitter);
            return;
        }

        throw new WasmAssemblerException("Unknown import type: " + im.getDescription());
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

    private void addMemoryImport(Import<MemoryImportDescription> im, ClassFileEmitter emitter) {
        memoryGeneratorFor(im).addMemory(null, im.getDescription().getType(), emitter);

        emitter.field(
                generateImportFieldNameForAttachment(im, "linked"),
                Visibility.PRIVATE,
                false,
                true,
                LINKED_MEMORY_TYPE,
                null
        );
    }

    private void addTypeImport(Import<TypeImportDescription> im, ClassFileEmitter emitter) {
        emitter.field(
                generateImportFieldName(im),
                Visibility.PRIVATE,
                false,
                true,
                LINKED_FUNCTION_TYPE,
                null
        );
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

        Import<MemoryImportDescription> memoryImport = im.tryCast(MemoryImportDescription.class);
        if (memoryImport != null) {
            emitLinkMemoryImport(memoryImport, context);
            return;
        }

        Import<TypeImportDescription> typeImport = im.tryCast(TypeImportDescription.class);
        if (typeImport != null) {
            emitLinkTypeImport(typeImport, context);
            return;
        }

        throw new WasmAssemblerException("Unknown import type " + im.getDescription());
    }

    private void emitLinkGlobalImport(Import<GlobalImportDescription> im, CodeEmitContext context)
            throws WasmAssemblerException {
        ValueType type = im.getDescription().getType().getValueType();
        CodeEmitter emitter = context.getEmitter();

        boolean isConst = im.getDescription().getType().getMutability() == GlobalType.Mutability.CONST;

        emitter.loadConstant(im.getModule());
        emitter.loadConstant(im.getName());
        CommonBytecodeGenerator.loadTypeReference(emitter, type);
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

        // Set the field to the result of the method invocation
        accessImportField(im, context, true);
    }

    private void emitLinkTableImport(Import<TableImportDescription> im, CodeEmitContext context)
            throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();
        ReferenceType type = im.getDescription().getType().getElementType();

        // Push the arguments for the method invocation, we expect the linker to be on top of the stack already

        emitter.loadConstant(im.getModule());

        emitter.loadConstant(im.getName());

        CommonBytecodeGenerator.loadTypeReference(emitter, type);
        CommonBytecodeGenerator.loadLimits(emitter, im.getDescription().getType().getLimits());

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

        // Set the field to the result of the method invocation
        emitter.loadLocal(context.getLocalVariables().getThis());
        emitter.op(Op.SWAP);
        emitter.accessField(
                emitter.getOwner(),
                generateImportFieldName(im),
                DefaultTableGenerator.LINKED_TABLE_TYPE,
                false,
                true
        );

    }

    private void emitLinkMemoryImport(Import<MemoryImportDescription> im, CodeEmitContext context)
            throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();
        MemoryType type = im.getDescription().getType();

        emitter.loadConstant(im.getModule());

        emitter.loadConstant(im.getName());

        CommonBytecodeGenerator.loadLimits(emitter, type.getLimits());

        emitter.invoke(
                RUNTIME_LINKER_TYPE,
                "linkMemory",
                new JavaType[]{
                        ObjectType.of(String.class),
                        ObjectType.of(String.class),
                        ObjectType.of(Limits.class),
                },
                LINKED_MEMORY_TYPE,
                InvokeType.INTERFACE,
                true
        );

        // Set the field to the result of the method invocation
        emitter.loadLocal(context.getLocalVariables().getThis());
        emitter.op(Op.SWAP);

        emitter.duplicate(2, 0);
        emitter.accessField(
                emitter.getOwner(),
                generateImportFieldNameForAttachment(im, "linked"),
                LINKED_MEMORY_TYPE,
                false,
                true
        );

        emitter.invoke(
                LINKED_MEMORY_TYPE,
                "asInternal",
                new JavaType[0],
                memoryGeneratorFor(im).getMemoryType(null),
                InvokeType.INTERFACE,
                true
        );
        emitter.accessField(
                emitter.getOwner(),
                generateImportFieldName(im),
                memoryGeneratorFor(im).getMemoryType(null),
                false,
                true
        );

    }

    private void emitLinkTypeImport(Import<TypeImportDescription> im, CodeEmitContext context)
            throws WasmAssemblerException {
        // WASM has named this confusingly, it's a function import in the end

        CodeEmitter emitter = context.getEmitter();
        int typeIndex = im.getDescription().getIndex();
        FunctionType type = context.getLookups().requireType(LargeArrayIndex.fromU32(typeIndex));

        emitter.loadConstant(im.getModule());
        emitter.loadConstant(im.getName());

        CommonBytecodeGenerator.loadFunctionType(emitter, type);

        emitter.invoke(
                RUNTIME_LINKER_TYPE,
                "linkFunction",
                new JavaType[]{
                        ObjectType.of(String.class),
                        ObjectType.of(String.class),
                        ObjectType.of(FunctionType.class),
                },
                LINKED_FUNCTION_TYPE,
                InvokeType.INTERFACE,
                true
        );

        // Set the field to the result of the method invocation
        emitter.loadLocal(context.getLocalVariables().getThis());
        emitter.op(Op.SWAP);
        emitter.accessField(
                emitter.getOwner(),
                generateImportFieldName(im),
                LINKED_FUNCTION_TYPE,
                false,
                true
        );
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
    }

    @Override
    public void emitSetGlobal(Import<GlobalImportDescription> im, CodeEmitContext context) throws WasmAssemblerException {
        // Load the linked global
        accessImportField(im, context, false);

        // We now have the global on the stack, swap it with the value
        CommonBytecodeGenerator.swap(context.getEmitter());

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
    public void emitMemoryGrow(Import<MemoryImportDescription> im, CodeEmitContext context) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();

        JavaLocal temporaryLocal = emitter.allocateLocal(PrimitiveType.INT);

        emitter.storeLocal(temporaryLocal);

        emitter.loadLocal(context.getLocalVariables().getThis());
        emitter.duplicate();

        emitter.accessField(
                emitter.getOwner(),
                generateImportFieldNameForAttachment(im, "linked"),
                LINKED_MEMORY_TYPE,
                false,
                false
        );

        emitter.duplicate();

        emitter.loadLocal(temporaryLocal);
        emitter.invoke(
                LINKED_MEMORY_TYPE,
                "grow",
                new JavaType[]{PrimitiveType.INT},
                PrimitiveType.BOOLEAN,
                InvokeType.INTERFACE,
                true
        );

        // Check if growing was successful
        CodeLabel endLabel = emitter.newLabel();
        CodeLabel successLabel = emitter.newLabel();
        emitter.jump(JumpCondition.INT_NOT_EQUAL_ZERO, successLabel);

        // Not successful, pop linked memory and this, push -1
        emitter.pop();
        emitter.pop();
        emitter.loadConstant(-1);

        emitter.jump(JumpCondition.ALWAYS, endLabel);

        // Successful, get and stow old memory size
        emitter.resolveLabel(successLabel);

        emitMemorySize(im, context);
        emitter.storeLocal(temporaryLocal);

        emitter.invoke(
                LINKED_MEMORY_TYPE,
                "asInternal",
                new JavaType[0],
                memoryGeneratorFor(im).getMemoryType(null),
                InvokeType.INTERFACE,
                true
        );
        emitter.accessField(
                emitter.getOwner(),
                generateImportFieldName(im),
                memoryGeneratorFor(im).getMemoryType(null),
                false,
                true
        );

        emitter.loadLocal(temporaryLocal);

        emitter.resolveLabel(endLabel);

        temporaryLocal.free();
    }

    @Override
    public void emitMemorySize(Import<MemoryImportDescription> im, CodeEmitContext context) throws WasmAssemblerException {
        memoryGeneratorFor(im).emitMemorySize(
                null,
                im.getDescription().getType(),
                context
        );
    }

    @Override
    public void emitMemoryInit(Import<MemoryImportDescription> im, LargeArrayIndex dataIndex, DataSegment segment, CodeEmitContext context) throws WasmAssemblerException {
        memoryGeneratorFor(im).emitMemoryInit(
                null,
                im.getDescription().getType(),
                dataIndex,
                segment,
                context
        );
    }

    @Override
    public void emitLoadMemoryReference(Import<MemoryImportDescription> im, CodeEmitContext context) throws WasmAssemblerException {
        memoryGeneratorFor(im).emitLoadMemoryReference(null, context);
    }

    @Override
    public void emitInvokeFunction(Import<TypeImportDescription> im, CodeEmitContext context) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();
        int typeIndex = im.getDescription().getIndex();
        FunctionType functionType = context.getLookups().requireType(LargeArrayIndex.fromU32(typeIndex));

        emitter.loadLocal(context.getLocalVariables().getThis());
        emitter.accessField(
                emitter.getOwner(),
                generateImportFieldName(im),
                LINKED_FUNCTION_TYPE,
                false,
                false
        );
        emitter.invoke(
                LINKED_FUNCTION_TYPE,
                "asMethodHandle",
                new JavaType[0],
                ObjectType.of(MethodHandle.class),
                InvokeType.INTERFACE,
                true
        );

        functionGenerator.emitInvokeFunctionByMethodHandle(functionType, context);
    }

    @Override
    public void emitLoadFunctionReference(Import<TypeImportDescription> im, CodeEmitContext context) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();

        emitter.doNew(ObjectType.of(FunctionReference.class));
        emitter.duplicate();

        emitter.loadLocal(context.getLocalVariables().getThis());
        emitter.accessField(
                emitter.getOwner(),
                generateImportFieldName(im),
                LINKED_FUNCTION_TYPE,
                false,
                false
        );

        emitter.invoke(
                ObjectType.of(FunctionReference.class),
                "<init>",
                new JavaType[]{LINKED_FUNCTION_TYPE},
                PrimitiveType.VOID,
                InvokeType.SPECIAL,
                false
        );
    }

    @Override
    public void makeFunctionExportable(Import<TypeImportDescription> im, ClassEmitContext context) {
        // No-op, import and export types are the same
    }

    @Override
    public void emitLoadFunctionExport(Import<TypeImportDescription> im, CodeEmitContext context) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();
        emitter.loadLocal(context.getLocalVariables().getThis());
        emitter.accessField(
                emitter.getOwner(),
                generateImportFieldName(im),
                LINKED_FUNCTION_TYPE,
                false,
                false
        );
    }

    @Override
    public void makeGlobalExportable(Import<GlobalImportDescription> im, ClassEmitContext context) {
        // No-op, import and export types are the same
    }

    @Override
    public void emitLoadGlobalExport(Import<GlobalImportDescription> im, CodeEmitContext context) throws WasmAssemblerException {
        ValueType type = im.getDescription().getType().getValueType();
        CodeEmitter emitter = context.getEmitter();

        boolean isConst = im.getDescription().getType().getMutability() == GlobalType.Mutability.CONST;

        emitter.loadLocal(context.getLocalVariables().getThis());
        emitter.accessField(
                emitter.getOwner(),
                generateImportFieldName(im),
                DefaultFieldTypeLookup.GLOBAL_IMPORT.select(type, isConst).getType(),
                false,
                false
        );
    }

    @Override
    public void makeMemoryExportable(Import<MemoryImportDescription> im, ClassEmitContext context) {
        // No-op, import and export types are the same
    }

    @Override
    public void emitLoadMemoryExport(Import<MemoryImportDescription> im, CodeEmitContext context) throws WasmAssemblerException {
        CodeEmitter emitter = context.getEmitter();
        emitter.loadLocal(context.getLocalVariables().getThis());
        emitter.accessField(
                emitter.getOwner(),
                generateImportFieldNameForAttachment(im, "linked"),
                LINKED_MEMORY_TYPE,
                false,
                false
        );
    }

    @Override
    public void makeTableExportable(Import<TableImportDescription> im, ClassEmitContext context) {
        // No-op, import and export types are the same
    }

    @Override
    public void emitLoadTableExport(Import<TableImportDescription> im, CodeEmitContext context) throws WasmAssemblerException {
        tableGeneratorFor(im).emitLoadTableExport(null, im.getDescription().getType(), context);
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
     * Generates a unique field name for an import attachment.
     *
     * @param im             the import to generate a field name for
     * @param attachmentName the name of the attachment
     * @return the generated field name
     */
    private String generateImportFieldNameForAttachment(Import<?> im, String attachmentName) {
        return generateImportFieldName(im) + "$" + attachmentName;
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
        CodeEmitter emitter = context.getEmitter();

        CommonBytecodeGenerator.loadThisBelow(emitter, context.getLocalVariables(), isSet ? 1 : 0);
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
