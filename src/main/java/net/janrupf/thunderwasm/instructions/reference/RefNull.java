package net.janrupf.thunderwasm.instructions.reference;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.eval.EvalContext;
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
    public void emitCode(CodeEmitContext context, Data data) throws WasmAssemblerException {
        context.getEmitter().loadConstant(null);
        context.getFrameState().pushOperand(data.getType());
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
