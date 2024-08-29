package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.types.NumberType;

import java.io.IOException;

public final class F32Const extends WasmInstruction<F32Const.Data> {
    public static final F32Const INSTANCE = new F32Const();

    private F32Const() {
        super("f32.const", (byte) 0x43);
    }

    @Override
    public Data readData(WasmLoader loader) throws IOException, InvalidModuleException {
        return new Data(loader.readF32());
    }

    @Override
    public void emitCode(CodeEmitContext context, Data data) throws WasmAssemblerException {
        context.getEmitter().loadConstant(data.value);
        context.getFrameState().pushOperand(NumberType.F32);
    }

    public static class Data implements WasmInstruction.Data {
        private final float value;

        private Data(float value) {
            this.value = value;
        }

        /**
         * Retrieves the value of the constant.
         *
         * @return the value
         */
        public float getValue() {
            return value;
        }

        @Override
        public String toString() {
            return Float.toString(value);
        }
    }
}
