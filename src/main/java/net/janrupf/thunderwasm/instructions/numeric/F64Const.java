package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.types.NumberType;

import java.io.IOException;

public final class F64Const extends WasmInstruction<F64Const.Data> {
    public static final F64Const INSTANCE = new F64Const();

    private F64Const() {
        super("f64.const", (byte) 0x44);
    }

    @Override
    public Data readData(WasmLoader loader) throws IOException, InvalidModuleException {
        return new Data(loader.readF64());
    }

    @Override
    public void emitCode(CodeEmitContext context, Data data) throws WasmAssemblerException {
        context.getEmitter().loadConstant(data.value);
        context.getFrameState().pushOperand(NumberType.F64);
    }

    public static final class Data implements WasmInstruction.Data {
        private final double value;

        private Data(double value) {
            this.value = value;
        }

        /**
         * Retrieves the value of the constant.
         *
         * @return the value
         */
        public double getValue() {
            return value;
        }

        @Override
        public String toString() {
            return Double.toString(value);
        }
    }
}
