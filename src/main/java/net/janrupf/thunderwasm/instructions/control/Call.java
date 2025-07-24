package net.janrupf.thunderwasm.instructions.control;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.imports.TypeImportDescription;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.instructions.control.internal.ControlHelper;
import net.janrupf.thunderwasm.lookup.FoundElement;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.types.FunctionType;

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

    @Override
    public void emitCode(CodeEmitContext context, Data data) throws WasmAssemblerException {
        FoundElement<Integer, TypeImportDescription> functionTypeIndex = context.getLookups().requireFunctionTypeIndex(
                LargeArrayIndex.fromU32(data.getFunctionIndex()));
        FunctionType functionType = context.getLookups().resovleFunctionType(functionTypeIndex);

        ControlHelper.popArguments(context, functionType);

        if (functionTypeIndex.isImport()) {
            context.getGenerators().getImportGenerator().emitInvokeFunction(functionTypeIndex.getImport(), context);
        } else {
            context.getGenerators().getFunctionGenerator().emitInvokeFunction(functionTypeIndex.getIndex(), functionType, context);
        }

        ControlHelper.pushReturnValues(context, functionType);

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
