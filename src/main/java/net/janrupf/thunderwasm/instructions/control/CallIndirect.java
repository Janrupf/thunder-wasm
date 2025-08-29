package net.janrupf.thunderwasm.instructions.control;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.imports.TableImportDescription;
import net.janrupf.thunderwasm.instructions.ProcessedInstruction;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.instructions.control.internal.ControlHelper;
import net.janrupf.thunderwasm.lookup.FoundElement;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.types.FunctionType;
import net.janrupf.thunderwasm.types.NumberType;
import net.janrupf.thunderwasm.types.ReferenceType;
import net.janrupf.thunderwasm.types.TableType;

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

    @Override
    public ProcessedInstruction processInputs(CodeEmitContext context, Data data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.I32);

        final FunctionType type = context.getLookups().requireType(LargeArrayIndex.fromU32(data.getTypeIndex()));
        final LargeArrayIndex tableIndex = LargeArrayIndex.fromU32(data.getTableIndex());
        final FoundElement<TableType, TableImportDescription> table = context.getLookups().requireTable(tableIndex);

        ReferenceType tableType;
        if (table.isImport()) {
            tableType = table.getImport().getDescription().getType().getElementType();
        } else {
            tableType = table.getElement().getElementType();
        }

        if (!tableType.equals(ReferenceType.FUNCREF)) {
            throw new WasmAssemblerException("Table for call_indirect must be of type funcref, but was " + tableType);
        }

        ControlHelper.popArguments(context, type);

        return new ProcessedInstruction() {
            @Override
            public void emitBytecode(CodeEmitContext context) throws WasmAssemblerException {
                context.getGenerators().getFunctionGenerator()
                        .emitInvokeFunctionIndirect(type, tableIndex, context);
            }

            @Override
            public void processOutputs(CodeEmitContext context) throws WasmAssemblerException {
                ControlHelper.pushReturnValues(context, type);
            }
        };
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
