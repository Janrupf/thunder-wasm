package net.janrupf.thunderwasm.instructions.variable;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.data.Global;
import net.janrupf.thunderwasm.imports.GlobalImportDescription;
import net.janrupf.thunderwasm.instructions.ProcessedInstruction;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.instructions.data.GlobalIndexData;
import net.janrupf.thunderwasm.lookup.FoundElement;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.types.GlobalType;
import net.janrupf.thunderwasm.types.ValueType;

import java.io.IOException;

public final class GlobalSet extends WasmInstruction<GlobalIndexData> {
    public static final GlobalSet INSTANCE = new GlobalSet();

    private GlobalSet() {
        super("global.set", (byte) 0x24);
    }

    @Override
    public GlobalIndexData readData(WasmLoader loader) throws IOException, InvalidModuleException {
        return GlobalIndexData.read(loader);
    }

    @Override
    public ProcessedInstruction processInputs(CodeEmitContext context, GlobalIndexData data) throws WasmAssemblerException {
        final FoundElement<Global, GlobalImportDescription> element = context.getLookups().requireGlobal(
                LargeArrayIndex.fromU32(data.getIndex())
        );
        
        final ValueType globalType;
        if (element.isImport()) {
            if (element.getImport().getDescription().getType().getMutability() != GlobalType.Mutability.VAR) {
                throw new WasmAssemblerException("Cannot set immutable global");
            }
            globalType = element.getImport().getDescription().getType().getValueType();
        } else {
            if (element.getElement().getType().getMutability() != GlobalType.Mutability.VAR) {
                throw new WasmAssemblerException("Cannot set immutable global");
            }
            globalType = element.getElement().getType().getValueType();
        }
        
        context.getFrameState().popOperand(globalType);
        
        return new ProcessedInstruction() {
            @Override
            public void emitBytecode(CodeEmitContext context) throws WasmAssemblerException {
                if (element.isImport()) {
                    context.getGenerators().getImportGenerator().emitSetGlobal(
                            element.getImport(),
                            context
                    );
                } else {
                    context.getGenerators().getGlobalGenerator().emitSetGlobal(
                            element.getIndex(),
                            element.getElement(),
                            context
                    );
                }
            }

            @Override
            public void processOutputs(CodeEmitContext context) {
            }
        };
    }
}
