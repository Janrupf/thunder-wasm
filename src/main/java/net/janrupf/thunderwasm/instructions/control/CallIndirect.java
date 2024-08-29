package net.janrupf.thunderwasm.instructions.control;

import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;

import java.io.IOException;

public final class CallIndirect extends WasmInstruction<CallIndirect.Data> {
    public static final CallIndirect INSTANCE = new CallIndirect();

    private CallIndirect() {
        super("call_indirect", (byte) 0x11);
    }

    @Override
    public Data readData(WasmLoader loader) throws IOException, InvalidModuleException {
        int typeIndex = loader.readU32();
        int tableIndex = loader.readU32();
        return new Data(typeIndex, tableIndex);
    }

    public static final class Data implements WasmInstruction.Data {
        private final int typeIndex;
        private final int tableIndex;

        private Data(int typeIndex, int tableIndex) {
            this.typeIndex = typeIndex;
            this.tableIndex = tableIndex;
        }

        /**
         * Retrieve the index of the function type to call.
         *
         * @return the index of the function type to call
         */
        public int getTypeIndex() {
            return typeIndex;
        }

        /**
         * Retrieve the index of the table to get the function reference from.
         *
         * @return the index of the table to get the function reference from
         */
        public int getTableIndex() {
            return tableIndex;
        }

        @Override
        public String toString() {
            return Integer.toUnsignedString(tableIndex, 10) + " " + Integer.toUnsignedString(typeIndex, 10);
        }
    }
}
