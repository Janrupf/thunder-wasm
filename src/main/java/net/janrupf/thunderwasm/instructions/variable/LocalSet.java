package net.janrupf.thunderwasm.instructions.variable;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.analysis.AnalysisContext;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitter;
import net.janrupf.thunderwasm.assembler.emitter.LocalVariables;
import net.janrupf.thunderwasm.instructions.ProcessedInstruction;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.instructions.control.internal.MultiValueHelper;
import net.janrupf.thunderwasm.instructions.data.LocalIndexData;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.types.ValueType;

import java.io.IOException;

public final class LocalSet extends WasmInstruction<LocalIndexData> {
    public static final LocalSet INSTANCE = new LocalSet();

    private LocalSet() {
        super("local.set", (byte) 0x21);
    }

    @Override
    public LocalIndexData readData(WasmLoader loader) throws IOException, InvalidModuleException {
        return LocalIndexData.read(loader);
    }

    @Override
    public ProcessedInstruction processInputs(CodeEmitContext context, LocalIndexData data) throws WasmAssemblerException {
        WasmFrameState frameState = context.getFrameState();
        final ValueType localType = frameState.requireLocal(data.getIndex());
        final int localIndex = data.getIndex();
        
        frameState.popOperand(localType);
        
        return new ProcessedInstruction() {
            @Override
            public void emitBytecode(CodeEmitContext context) throws WasmAssemblerException {
                CodeEmitter emitter = context.getEmitter();
                
                if (context.getLocalVariables().getType(localIndex) == LocalVariables.LocalType.HEAP) {
                    emitter.loadLocal(context.getLocalVariables().getHeapLocals());
                    
                    LocalVariables.HeapLocal l = context.getLocalVariables().requireHeapById(localIndex);
                    MultiValueHelper.emitSetByIndex(emitter, l.getType(), l.getIndex());
                } else {
                    emitter.storeLocal(context.getLocalVariables().requireById(localIndex));
                }
            }

            @Override
            public void processOutputs(CodeEmitContext context) {
            }
        };
    }

    @Override
    public void runAnalysis(AnalysisContext context, LocalIndexData data) {
        context.getLocalVariableUsage().write(data.getIndex());
    }
}
