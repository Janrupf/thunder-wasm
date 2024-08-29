package net.janrupf.thunderwasm.instructions.numeric;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitter;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.types.NumberType;

import java.io.IOException;

public final class I32Const extends WasmInstruction<I32Const.Data> {
    public static final I32Const INSTANCE = new I32Const();

    private I32Const() {
        super("i32.const", (byte) 0x41);
    }

    @Override
    public Data readData(WasmLoader loader) throws IOException, InvalidModuleException {
        return new Data(loader.readS32());
    }

    @Override
    public void emitCode(WasmFrameState frameState, CodeEmitter emitter, Data data) throws WasmAssemblerException {
        emitter.loadConstant(data.value);
        frameState.pushOperand(NumberType.I32);
    }

    public static class Data implements WasmInstruction.Data {
        private final int value;

        private Data(int value) {
            this.value = value;
        }

        /**
         * Retrieves the value of the constant.
         *
         * @return the value
         */
        public int getValue() {
            return value;
        }

        @Override
        public String toString() {
            return Integer.toString(value);
        }
    }
}
