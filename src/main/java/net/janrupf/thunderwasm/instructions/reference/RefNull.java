package net.janrupf.thunderwasm.instructions.reference;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.types.ObjectType;
import net.janrupf.thunderwasm.eval.EvalContext;
import net.janrupf.thunderwasm.instructions.ProcessedInstruction;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.types.ReferenceType;

import java.io.IOException;

public final class RefNull extends WasmInstruction<RefNull.Data> {
    public static final RefNull INSTANCE = new RefNull();

    private RefNull() {
        super("ref.null", (byte) 0xD0);
    }

    @Override
    public Data readData(WasmLoader loader) throws IOException, InvalidModuleException {
        ReferenceType type = loader.readReferenceType();
        return new Data(type);
    }

    @Override
    public ProcessedInstruction processInputs(CodeEmitContext context, Data data) throws WasmAssemblerException {
        final ReferenceType outputType = data.getType();
        
        return new ProcessedInstruction() {
            @Override
            public void emitBytecode(CodeEmitContext context) throws WasmAssemblerException {
                context.getEmitter().loadNull(ObjectType.OBJECT);
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
    public void eval(EvalContext context, Data data) throws WasmAssemblerException {
        context.getFrameState().push(data.getType(), null);
    }

    public static final class Data implements WasmInstruction.Data {
        private final ReferenceType type;

        private Data(ReferenceType type) {
            this.type = type;
        }

        /**
         * The reference type to create.
         *
         * @return the reference type
         */
        public ReferenceType getType() {
            return type;
        }
    }
}
