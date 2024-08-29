package net.janrupf.thunderwasm.instructions.control;

import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;

import java.io.IOException;

public final class Call extends WasmInstruction<Call.Data> {
    public static final Call INSTANCE = new Call();

    private Call() {
        super("call", (byte) 0x10);
    }

    @Override
    public Data readData(WasmLoader loader) throws IOException, InvalidModuleException {
        int functionIndex = loader.readU32();
        return new Data(functionIndex);
    }

    public static final class Data implements WasmInstruction.Data {
        private final int functionIndex;

        private Data(int functionIndex) {
            this.functionIndex = functionIndex;
        }

        /**
         * Retrieve the index of the function to call.
         *
         * @return the index of the function to call
         */
        public int getFunctionIndex() {
            return functionIndex;
        }

        @Override
        public String toString() {
            return Integer.toUnsignedString(functionIndex, 10);
        }
    }
}
