package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitter;
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
    public void emitCode(WasmFrameState frameState, CodeEmitter emitter, Data data) throws WasmAssemblerException {
        emitter.loadConstant(data.value);
        frameState.pushOperand(NumberType.I64);
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
