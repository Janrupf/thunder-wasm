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
import net.janrupf.thunderwasm.types.ValueType;

import java.io.IOException;

public final class LocalGet extends WasmInstruction<LocalIndexData> {
    public static final LocalGet INSTANCE = new LocalGet();

    private LocalGet() {
        super("local.get", (byte) 0x20);
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

        ValueType type = frameState.requireLocal(data.getIndex());

        if (context.getLocalVariables().getType(data.getIndex()) == LocalVariables.LocalType.HEAP) {
            emitter.loadLocal(context.getLocalVariables().getHeapLocals());

            LocalVariables.HeapLocal l = context.getLocalVariables().requireHeapById(data.getIndex());
            MultiValueHelper.emitGetByIndex(emitter, l.getType(), l.getIndex());
        } else {
            emitter.loadLocal(context.getLocalVariables().requireById(data.getIndex()));
        }

        frameState.pushOperand(type);
    }

    @Override
    public void runAnalysis(AnalysisContext context, LocalIndexData data) {
        context.getLocalVariableUsage().read(data.getIndex());
    }
}
