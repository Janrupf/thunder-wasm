package net.janrupf.thunderwasm.instructions.variable;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.WasmTypeConverter;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitter;
import net.janrupf.thunderwasm.data.Global;
import net.janrupf.thunderwasm.imports.GlobalImportDescription;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.instructions.data.GlobalIndexData;
import net.janrupf.thunderwasm.instructions.data.LocalIndexData;
import net.janrupf.thunderwasm.lookup.FoundElement;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.module.section.GlobalSection;
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
    public void emitCode(
            CodeEmitContext context,
            GlobalIndexData data
    ) throws WasmAssemblerException {
        FoundElement<Global, GlobalImportDescription> gElement = context.getLookups().requireGlobal(
                LargeArrayIndex.fromU32(data.getIndex())
        );

        if (gElement.isImport()) {
            context.getGenerators().getImportGenerator().emitGlobalGet(
                    gElement.getImport(),
                    context
            );
        } else {
            context.getGenerators().getGlobalGenerator().emitGetGlobal(
                    gElement.getIndex(),
                    gElement.getElement(),
                    context
            );
        }
    }
}
