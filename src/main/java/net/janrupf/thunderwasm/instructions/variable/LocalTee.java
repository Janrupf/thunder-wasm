package net.janrupf.thunderwasm.instructions.variable;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.analysis.AnalysisContext;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitter;
import net.janrupf.thunderwasm.assembler.emitter.LocalVariables;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.instructions.control.internal.MultiValueHelper;
import net.janrupf.thunderwasm.instructions.data.LocalIndexData;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;

import java.io.IOException;

public final class LocalTee extends WasmInstruction<LocalIndexData> {
    public static final LocalTee INSTANCE = new LocalTee();

    private LocalTee() {
        super("local.tee", (byte) 0x22);
    }

    @Override
    public LocalIndexData readData(WasmLoader loader) throws IOException, InvalidModuleException {
        return LocalIndexData.read(loader);
    }

    @Override
    public void emitCode(
            CodeEmitContext context, LocalIndexData data
    ) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        CodeEmitter emitter = context.getEmitter();

        frameState.requireOperand(frameState.requireLocal(data.getIndex()));

        emitter.duplicate();

        if (context.getLocalVariables().getType(data.getIndex()) == LocalVariables.LocalType.HEAP) {
            emitter.loadLocal(context.getLocalVariables().getHeapLocals());

            LocalVariables.HeapLocal l = context.getLocalVariables().requireHeapById(data.getIndex());
            MultiValueHelper.emitSetByIndex(emitter, l.getType(), l.getIndex());
        } else {
            emitter.storeLocal(context.getLocalVariables().requireById(data.getIndex()));
        }
    }

    @Override
    public void runAnalysis(AnalysisContext context, LocalIndexData data) {
        context.getLocalVariableUsage().write(data.getIndex());
    }
}
