package net.janrupf.thunderwasm.instructions.memory;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.imports.MemoryImportDescription;
import net.janrupf.thunderwasm.instructions.memory.base.PlainMemory;
import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryLoad;
import net.janrupf.thunderwasm.instructions.memory.base.PlainMemoryStore;
import net.janrupf.thunderwasm.lookup.FoundElement;
import net.janrupf.thunderwasm.types.MemoryType;
import net.janrupf.thunderwasm.types.NumberType;

class MemoryInstructionHelper {
    private final FoundElement<MemoryType, MemoryImportDescription> element;
    private final CodeEmitContext emitContext;

    MemoryInstructionHelper(FoundElement<MemoryType, MemoryImportDescription> element, CodeEmitContext emitContext) {
        this.element = element;
        this.emitContext = emitContext;
    }

    /**
     * Retrieve the memory type.
     *
     * @return the memory type
     */
    public MemoryType getMemoryType() {
        if (element.isImport()) {
            return element.getImport().getDescription().getType();
        } else {
            return element.getElement();
        }
    }

    /**
     * Retrieves the backing Java type for the memory.
     *
     * @return the backing Java type
     */
    public ObjectType getJavaMemoryType() {
        if (element.isImport()) {
            return emitContext.getGenerators().getImportGenerator().getMemoryType(element.getImport());
        } else {
            return emitContext.getGenerators().getMemoryGenerator().getMemoryType(element.getIndex());
        }
    }

    /**
     * Emit a memory byte load operation for fallback implementations.
     * Expects offset on stack, pushes byte value.
     *
     * @throws WasmAssemblerException if an error occurs
     */
    public void emitMemoryLoadByte() throws WasmAssemblerException {
        PlainMemory.Memarg memarg = new PlainMemory.Memarg(0, 0);
        
        if (element.isImport()) {
            emitContext.getGenerators().getImportGenerator().emitMemoryLoad(
                    element.getImport(),
                    NumberType.I32,
                    memarg,
                    PlainMemoryLoad.LoadType.UNSIGNED_8,
                    emitContext
            );
        } else {
            emitContext.getGenerators().getMemoryGenerator().emitLoad(
                    element.getIndex(),
                    element.getElement(),
                    NumberType.I32,
                    memarg,
                    PlainMemoryLoad.LoadType.UNSIGNED_8,
                    emitContext
            );
        }
    }

    /**
     * Emit a memory byte store operation for fallback implementations.
     * Expects value and offset on stack.
     *
     * @throws WasmAssemblerException if an error occurs
     */
    public void emitMemoryStoreByte() throws WasmAssemblerException {
        PlainMemory.Memarg memarg = new PlainMemory.Memarg(0, 0);
        
        if (element.isImport()) {
            emitContext.getGenerators().getImportGenerator().emitMemoryStore(
                    element.getImport(),
                    NumberType.I32,
                    memarg,
                    PlainMemoryStore.StoreType.BIT_8,
                    emitContext
            );
        } else {
            emitContext.getGenerators().getMemoryGenerator().emitStore(
                    element.getIndex(),
                    element.getElement(),
                    NumberType.I32,
                    memarg,
                    PlainMemoryStore.StoreType.BIT_8,
                    emitContext
            );
        }
    }

    /**
     * Emit an operation loading the memory reference.
     *
     * @throws WasmAssemblerException if an error occurs
     */
    public void emitLoadMemoryReference() throws WasmAssemblerException {
        if (element.isImport()) {
            emitContext.getGenerators().getImportGenerator().emitLoadMemoryReference(
                    element.getImport(),
                    emitContext
            );
        } else {
            emitContext.getGenerators().getMemoryGenerator().emitLoadMemoryReference(
                    element.getIndex(),
                    emitContext
            );
        }
    }

    /**
     * Emit an operation retrieving the memory size.
     *
     * @throws WasmAssemblerException if an error occurs
     */
    public void emitMemorySize() throws WasmAssemblerException {
        if (element.isImport()) {
            emitContext.getGenerators().getImportGenerator().emitMemorySize(
                    element.getImport(),
                    emitContext
            );
        } else {
            emitContext.getGenerators().getMemoryGenerator().emitMemorySize(
                    element.getIndex(),
                    element.getElement(),
                    emitContext
            );
        }
    }
}