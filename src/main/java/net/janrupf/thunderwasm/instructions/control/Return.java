package net.janrupf.thunderwasm.instructions.control;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.analysis.AnalysisContext;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.instructions.EmptyInstructionData;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.instructions.control.internal.BlockHelper;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.types.ValueType;

import java.io.IOException;
import java.util.List;

public final class Return extends WasmInstruction<EmptyInstructionData> {
    public static final Return INSTANCE = new Return();

    private Return() {
        super("return", (byte) 0x0F);
    }

    @Override
    public EmptyInstructionData readData(WasmLoader loader) throws IOException, InvalidModuleException {
        return EmptyInstructionData.INSTANCE;
    }

    @Override
    public void emitCode(CodeEmitContext context, EmptyInstructionData data) throws WasmAssemblerException {
        BlockHelper.emitDirectReturn(context);
    }

    @Override
    public void runAnalysis(AnalysisContext context, EmptyInstructionData data) {
        context.markForDirectReturn();
    }
}
