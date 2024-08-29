package net.janrupf.thunderwasm.instructions.variable;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.data.Global;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.instructions.data.GlobalIndexData;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.module.section.GlobalSection;
import net.janrupf.thunderwasm.types.GlobalType;

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
    public void emitCode(
            CodeEmitContext context,
            GlobalIndexData data
    ) throws WasmAssemblerException {
        LargeArray<Global> globals = context.getLookups()
                .requireSingleSection(GlobalSection.class, (byte) 0x06).getGlobals();
        LargeArrayIndex index = LargeArrayIndex.fromU32(data.getIndex());

        if (!globals.isValid(index)) {
            throw new WasmAssemblerException("Invalid global index");
        }

        Global global = globals.get(index);
        if (global.getType().getMutability() != GlobalType.Mutability.VAR) {
            throw new WasmAssemblerException("Cannot set immutable global");
        }

        context.getGenerators().getGlobalGenerator().emitSetGlobal(
                index,
                global,
                context
        );
    }
}
