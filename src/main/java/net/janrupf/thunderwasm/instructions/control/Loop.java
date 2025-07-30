package net.janrupf.thunderwasm.instructions.control;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.analysis.AnalysisContext;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.instructions.control.internal.BlockHelper;
import net.janrupf.thunderwasm.instructions.control.internal.ControlHelper;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;

import java.io.IOException;

public final class Loop extends WasmInstruction<BlockData> {
    public static final Loop INSTANCE = new Loop();

    private Loop() {
        super("loop", (byte) 0x03);
    }

    @Override
    public BlockData readData(WasmLoader loader) throws IOException, InvalidModuleException {
        return BlockData.read(loader, false);
    }

    @Override
    public void emitCode(CodeEmitContext context, BlockData data) throws WasmAssemblerException {
        BlockHelper.emitInvokeBlock(context, data, true, true);
    }

    @Override
    public void runAnalysis(AnalysisContext context, BlockData data) throws WasmAssemblerException {
        AnalysisContext subcontext = context.branchForExpression(data.getPrimaryExpression());
        subcontext.run();
    }
}
