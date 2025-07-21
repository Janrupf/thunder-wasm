package net.janrupf.thunderwasm.instructions.reference;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.CommonBytecodeGenerator;
import net.janrupf.thunderwasm.eval.EvalContext;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.runtime.FunctionReference;
import net.janrupf.thunderwasm.types.ReferenceType;

import java.io.IOException;

public final class RefFunc extends WasmInstruction<RefFunc.Data> {
    public static final RefFunc INSTANCE = new RefFunc();

    private RefFunc() {
        super("ref.func", (byte) 0xD2);
    }

    @Override
    public Data readData(WasmLoader loader) throws IOException, InvalidModuleException {
        int functionIndex = loader.readU32();
        return new Data(functionIndex);
    }

    @Override
    public void emitCode(CodeEmitContext context, Data data) throws WasmAssemblerException {
        context.getFrameState().pushOperand(ReferenceType.FUNCREF);
        CommonBytecodeGenerator.loadConstant(context.getEmitter(), ReferenceType.FUNCREF, FunctionReference.of(data.getFunctionIndex()));
    }

    @Override
    public boolean isConst() {
        return true;
    }

    @Override
    public void eval(EvalContext context, Data data) {
        context.getFrameState().push(ReferenceType.FUNCREF, FunctionReference.of(data.getFunctionIndex()));
    }

    public static final class Data implements WasmInstruction.Data {
        private final int functionIndex;

        private Data(int functionIndex) {
            this.functionIndex = functionIndex;
        }

        /**
         * The index of the function to create a reference to.
         *
         * @return the function index
         */
        public int getFunctionIndex() {
            return functionIndex;
        }
    }
}
