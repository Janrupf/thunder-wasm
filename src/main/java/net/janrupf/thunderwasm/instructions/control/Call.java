package net.janrupf.thunderwasm.instructions.control;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.imports.TypeImportDescription;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
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

        if (functionTypeIndex.isImport()) {
            throw new WasmAssemblerException("Imported functions not supported yet");
        } else {
            LargeArrayIndex localFunctionTypeIndex = LargeArrayIndex.fromU32(functionTypeIndex.getElement());
            FunctionType functionType = context.getLookups().requireType(localFunctionTypeIndex);

            popArguments(context, functionType);
            context.getGenerators().getFunctionGenerator().emitInvokeFunction(functionTypeIndex.getIndex(), functionType, context);
            pushReturnValues(context, functionType);
        }
    }

    private void popArguments(CodeEmitContext context, FunctionType functionType) throws WasmAssemblerException {
        // Pop the arguments from the stack
        for (LargeArrayIndex i = LargeArrayIndex.ZERO; i.compareTo(functionType.getInputs().largeLength()) < 0; i = i.add(1)) {
            context.getFrameState().popOperand(functionType.getInputs().get(i));
        }
    }

    private void pushReturnValues(CodeEmitContext context, FunctionType functionType) throws WasmAssemblerException {
        // Push the return values onto the stack
        LargeArrayIndex i = functionType.getOutputs().largeLength();
        while (i.compareTo(LargeArrayIndex.ZERO) > 0) {
            i = i.subtract(1);
            context.getFrameState().pushOperand(functionType.getOutputs().get(i));
        }
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
