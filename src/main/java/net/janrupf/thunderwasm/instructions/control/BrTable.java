package net.janrupf.thunderwasm.instructions.control;

import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.WasmFrameState;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitContext;
import net.janrupf.thunderwasm.assembler.emitter.CodeEmitter;
import net.janrupf.thunderwasm.assembler.emitter.CodeLabel;
import net.janrupf.thunderwasm.assembler.emitter.JumpCondition;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.instructions.control.internal.ControlHelper;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.module.encoding.LargeIntArray;
import net.janrupf.thunderwasm.types.NumberType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class BrTable extends WasmInstruction<BrTable.Data> {
    public static final BrTable INSTANCE = new BrTable();

    private BrTable() {
        super("br_table", (byte) 0x0E);
    }

    @Override
    public Data readData(WasmLoader loader) throws IOException, InvalidModuleException {
        LargeIntArray branchLabels = loader.readU32Vec();
        int defaultLabel = loader.readU32();

        return new Data(branchLabels, defaultLabel);
    }

    @Override
    public void emitCode(CodeEmitContext context, Data data) throws WasmAssemblerException {
        context.getFrameState().popOperand(NumberType.I32);

        CodeEmitter emitter = context.getEmitter();

        int[] branchLabels = data.getBranchLabels().asFlatArray();
        if (branchLabels == null) {
            throw new WasmAssemblerException("Too many branch labels");
        }

        Map<Integer, CodeLabel> targetLabels = new HashMap<>();

        int defaultDepth = data.getDefaultLabel();
        targetLabels.put(defaultDepth, emitter.newLabel());

        CodeLabel defaultCodeLabel = targetLabels.get(defaultDepth);
        CodeLabel[] branchCodeLabels = new CodeLabel[branchLabels.length];

        // Collect all the labels that will possibly be branched to
        for (int i = 0; i < branchLabels.length; i++) {
            int depth = branchLabels[i];
            if (!targetLabels.containsKey(depth)) {
                targetLabels.put(depth, emitter.newLabel());
            }

            branchCodeLabels[i] = targetLabels.get(depth);
        }

        emitter.tableSwitch(0, defaultCodeLabel, branchCodeLabels);

        // Now generate all the cases
        for (Map.Entry<Integer, CodeLabel> entry : targetLabels.entrySet()) {
            int depth = entry.getKey();
            CodeLabel codeLabel = entry.getValue();

            WasmFrameState preBranchFrameState = context.getFrameState().branch();

            emitter.resolveLabel(codeLabel);
            CodeLabel jumpTarget = ControlHelper.emitCleanStackForBlockLabel(context, depth);
            emitter.jump(JumpCondition.ALWAYS, jumpTarget);

            context.restoreFrameStateAfterBranch(preBranchFrameState);
        }

        context.getFrameState().markUnreachable();
    }

    public static final class Data implements WasmInstruction.Data {
        private final LargeIntArray branchLabels;
        private final int defaultLabel;

        private Data(LargeIntArray branchLabels, int defaultLabel) {
            this.branchLabels = branchLabels;
            this.defaultLabel = defaultLabel;
        }

        /**
         * Retrieves the branch labels.
         *
         * @return the branch labels
         */
        public LargeIntArray getBranchLabels() {
            return branchLabels;
        }

        /**
         * Retrieves the default label.
         *
         * @return the default label
         */
        public int getDefaultLabel() {
            return defaultLabel;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("[");
            for (LargeArrayIndex i = LargeArrayIndex.ZERO; i.toU64() < branchLabels.length(); i = i.add(1)) {
                if (i.compareTo(LargeArrayIndex.ZERO) > 0) {
                    builder.append(", ");
                }

                builder.append(i);
                builder.append(" -> ");
                builder.append(branchLabels.get(i));
            }

            if (branchLabels.length() > 0) {
                builder.append(", ");
            }

            builder.append("default -> ");
            builder.append(defaultLabel);
            builder.append("]");
            return builder.toString();
        }
    }
}
