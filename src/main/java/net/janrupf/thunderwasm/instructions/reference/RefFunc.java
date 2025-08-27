package net.janrupf.thunderwasm.instructions.reference;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.CommonBytecodeGenerator;
import net.janrupf.thunderwasm.eval.EvalContext;
import net.janrupf.thunderwasm.imports.TypeImportDescription;
import net.janrupf.thunderwasm.instructions.ProcessedInstruction;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.lookup.FoundElement;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.runtime.UnresolvedFunctionReference;
import net.janrupf.thunderwasm.types.FunctionType;
import net.janrupf.thunderwasm.types.ReferenceType;
import net.janrupf.thunderwasm.types.ValueType;

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
    public ProcessedInstruction processInputs(CodeEmitContext context, Data data) throws WasmAssemblerException {
        final FoundElement<Integer, TypeImportDescription> functionTypeIndex = context.getLookups().requireFunctionTypeIndex(
                LargeArrayIndex.fromU32(data.getFunctionIndex()));
        final FunctionType functionType = context.getLookups().resovleFunctionType(functionTypeIndex);
        final ValueType outputType = ReferenceType.FUNCREF;
        
        return new ProcessedInstruction() {
            @Override
            public void emitBytecode(CodeEmitContext context) throws WasmAssemblerException {
                if (functionTypeIndex.isImport()) {
                    context.getGenerators().getImportGenerator().emitLoadFunctionReference(functionTypeIndex.getImport(), context);
                } else {
                    context.getGenerators().getFunctionGenerator().emitLoadFunctionReference(functionTypeIndex.getIndex(), functionType, context);
                }
            }
            
            @Override
            public void processOutputs(CodeEmitContext context) throws WasmAssemblerException {
                context.getFrameState().pushOperand(outputType);
            }
        };
    }

    @Override
    public boolean isConst() {
        return true;
    }

    @Override
    public void eval(EvalContext context, Data data) {
        context.getFrameState().push(ReferenceType.FUNCREF, UnresolvedFunctionReference.of(data.getFunctionIndex()));
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
