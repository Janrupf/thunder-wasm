package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.eval.EvalContext;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.types.NumberType;

import java.io.IOException;

public final class I64Const extends WasmInstruction<I64Const.Data> {
    public static final I64Const INSTANCE = new I64Const();

    private I64Const() {
        super("i64.const", (byte) 0x42);
    }

    @Override
    public Data readData(WasmLoader loader) throws IOException, InvalidModuleException {
        return new Data(loader.readS64());
    }

    @Override
    public void emitCode(CodeEmitContext context, Data data) throws WasmAssemblerException {
        context.getEmitter().loadConstant(data.value);
        context.getFrameState().pushOperand(NumberType.I64);
    }

    @Override
    public boolean isConst() {
        return true;
    }

    @Override
    public void eval(EvalContext context, Data data) {
        context.getFrameState().push(NumberType.I64, data.getValue());
    }

    public static class Data implements WasmInstruction.Data {
        private final long value;

        private Data(long value) {
            this.value = value;
        }

        /**
         * Retrieves the value of the constant.
         *
         * @return the value
         */
        public long getValue() {
            return value;
        }

        @Override
        public String toString() {
            return Long.toString(value);
        }
    }
}
