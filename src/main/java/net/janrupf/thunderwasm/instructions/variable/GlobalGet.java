package net.janrupf.thunderwasm.instructions.variable;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.data.Global;
import net.janrupf.thunderwasm.eval.EvalContext;
import net.janrupf.thunderwasm.imports.GlobalImportDescription;
import net.janrupf.thunderwasm.imports.Import;
import net.janrupf.thunderwasm.instructions.ProcessedInstruction;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.instructions.data.GlobalIndexData;
import net.janrupf.thunderwasm.lookup.FoundElement;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.runtime.ImportedGlobalValueReference;
import net.janrupf.thunderwasm.types.GlobalType;
import net.janrupf.thunderwasm.types.ValueType;

import java.io.IOException;

public final class GlobalGet extends WasmInstruction<GlobalIndexData> {
    public static final GlobalGet INSTANCE = new GlobalGet();

    private GlobalGet() {
        super("global.get", (byte) 0x23);
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
        final ValueType outputType = element.isImport() 
                ? element.getImport().getDescription().getType().getValueType()
                : element.getElement().getType().getValueType();
        
        return new ProcessedInstruction() {
            @Override
            public void emitBytecode(CodeEmitContext context) throws WasmAssemblerException {
                if (element.isImport()) {
                    context.getGenerators().getImportGenerator().emitGetGlobal(
                            element.getImport(),
                            context
                    );
                } else {
                    context.getGenerators().getGlobalGenerator().emitGetGlobal(
                            element.getIndex(),
                            element.getElement(),
                            context
                    );
                }
            }

            @Override
            public void processOutputs(CodeEmitContext context) throws WasmAssemblerException {
                context.getFrameState().pushOperand(outputType);
            }
        };
    }

    @Override
    public boolean isConst() {
        return true;
    }

    @Override
    public void eval(EvalContext context, GlobalIndexData data) throws WasmAssemblerException {
        FoundElement<Global, GlobalImportDescription> gElement = context.getLookups().requireGlobal(
                LargeArrayIndex.fromU32(data.getIndex())
        );

        if (gElement.isImport()) {
            Import<GlobalImportDescription> importedGlobal = gElement.getImport();

            if (importedGlobal.getDescription().getType().getMutability() != GlobalType.Mutability.CONST) {
                throw new WasmAssemblerException("Cannot evaluate mutable imported global");
            }

           context.getFrameState().push(
                   importedGlobal.getDescription().getType().getValueType(),
                   new ImportedGlobalValueReference(importedGlobal)
           );
        } else {
            if (!context.doesAllowReferencingNonImportGlobals()) {
                throw new WasmAssemblerException("Cannot evaluate non-imported global in this context");
            }

            Global g = gElement.getElement();

            if (g.getType().getMutability() != GlobalType.Mutability.CONST) {
                throw new WasmAssemblerException("Cannot evaluate mutable global");
            }

            ValueType type = g.getType().getValueType();

            Object value = context.deriveFresh(false).evalSingleValue(g.getInit(), true, type);
            context.getFrameState().push(type, value);
        }
    }
}
