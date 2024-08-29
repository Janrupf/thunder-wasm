package net.janrupf.thunderwasm.instructions.reference;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitter;
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
    public void emitCode(WasmFrameState frameState, CodeEmitter emitter, Data data) throws WasmAssemblerException {
        emitter.loadConstant(null);
        frameState.pushOperand(data.getType());
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
