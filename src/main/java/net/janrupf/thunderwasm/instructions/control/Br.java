package net.janrupf.thunderwasm.instructions.control;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.CodeLabel;
import net.janrupf.thunderwasm.assembler.emitter.JumpCondition;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.instructions.control.internal.ControlHelper;
import net.janrupf.thunderwasm.module.WasmLoader;

import java.io.IOException;

public final class Br extends WasmInstruction<LabelData> {
    public static final Br INSTANCE = new Br();

    private Br() {
        super("br", (byte) 0x0C);
    }

    @Override
    public LabelData readData(WasmLoader loader) throws IOException {
        return LabelData.read(loader);
    }

    @Override
    public void emitCode(CodeEmitContext context, LabelData data) throws WasmAssemblerException {
        CodeLabel label = ControlHelper.emitCleanStackForBlockLabel(context, data.getLabelIndex());
        context.getEmitter().jump(JumpCondition.ALWAYS, label);
        context.getFrameState().markUnreachable();
    }
}
