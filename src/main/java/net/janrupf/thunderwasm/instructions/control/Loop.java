package net.janrupf.thunderwasm.instructions.control;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
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
        ControlHelper.emitPushBlock(context, data.getType(), true);
        ControlHelper.emitExpression(context, data.getPrimaryExpression());
        ControlHelper.emitPopBlock(context, data.getType());
    }
}
