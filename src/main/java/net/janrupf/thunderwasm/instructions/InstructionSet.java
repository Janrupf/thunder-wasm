package net.janrupf.thunderwasm.instructions;

import net.janrupf.thunderwasm.instructions.control.*;
import net.janrupf.thunderwasm.instructions.memory.DataDrop;
import net.janrupf.thunderwasm.instructions.numeric.*;
import net.janrupf.thunderwasm.instructions.parametric.Drop;
import net.janrupf.thunderwasm.instructions.parametric.Select;
import net.janrupf.thunderwasm.instructions.reference.RefFunc;
import net.janrupf.thunderwasm.instructions.reference.RefIsNull;
import net.janrupf.thunderwasm.instructions.reference.RefNull;
import net.janrupf.thunderwasm.instructions.variable.LocalGet;
import net.janrupf.thunderwasm.instructions.variable.LocalSet;
import net.janrupf.thunderwasm.instructions.variable.LocalTee;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The instruction set that can be executed by the virtual machine.
 */
public final class InstructionSet {
    private final Set<WasmInstruction<?>> instructions;

    public InstructionSet(Set<WasmInstruction<?>> instructions) {
        this.instructions = instructions;
    }

    /**
     * Create a new instruction set with the given instructions.
     *
     * @param instructions the instructions to include in the set
     * @return the new instruction set
     */
    public static InstructionSet of(WasmInstruction<?>... instructions) {
        Set<WasmInstruction<?>> instructionSet = new HashSet<>();
        Collections.addAll(instructionSet, instructions);

        return new InstructionSet(instructionSet);
    }

    /**
     * Retrieve all instructions in this set.
     *
     * @return the instructions in this set
     */
    public Set<WasmInstruction<?>> getInstructions() {
        return Collections.unmodifiableSet(instructions);
    }

    /**
     * The base WASM specification instruction set.
     */
    public static final InstructionSet BASE = InstructionSet.of(
            // Reference instructions
            RefNull.INSTANCE,
            RefIsNull.INSTANCE,
            RefFunc.INSTANCE,

            // Parametric instructions
            Drop.INSTANCE,
            Select.VARIANT_WITHOUT_TYPES,
            Select.VARIANT_WITH_TYPES,

            // Control instructions
            Unreachable.INSTANCE,
            Nop.INSTANCE,
            Block.INSTANCE,
            Loop.INSTANCE,
            If.INSTANCE,
            Br.INSTANCE,
            BrIf.INSTANCE,
            BrTable.INSTANCE,
            Return.INSTANCE,
            Call.INSTANCE,
            CallIndirect.INSTANCE,

            // Variable instructions
            LocalGet.INSTANCE,
            LocalSet.INSTANCE,
            LocalTee.INSTANCE,

            // Numeric instructions
            I32Const.INSTANCE,
            I64Const.INSTANCE,
            F32Const.INSTANCE,
            F64Const.INSTANCE,

            I32Eqz.INSTANCE,
            I32Eq.INSTANCE,
            I32Ne.INSTANCE,
            I32LtS.INSTANCE,
            I32LtU.INSTANCE,
            I32GtS.INSTANCE,
            I32GtU.INSTANCE,
            I32LeS.INSTANCE,
            I32LeU.INSTANCE,
            I32GeS.INSTANCE,
            I32GeU.INSTANCE,

            I64Eqz.INSTANCE,
            I64Eq.INSTANCE,
            I64Ne.INSTANCE,
            I64LtS.INSTANCE,
            I64LtU.INSTANCE,
            I64GtS.INSTANCE,
            I64GtU.INSTANCE,
            I64LeS.INSTANCE,
            I64LeU.INSTANCE,
            I64GeS.INSTANCE,
            I64GeU.INSTANCE,

            F32Eq.INSTANCE,
            F32Ne.INSTANCE,
            F32Lt.INSTANCE,
            F32Gt.INSTANCE,
            F32Le.INSTANCE,
            F32Ge.INSTANCE,

            F64Eq.INSTANCE,
            F64Ne.INSTANCE,
            F64Lt.INSTANCE,
            F64Gt.INSTANCE,
            F64Le.INSTANCE,
            F64Ge.INSTANCE,

            I32Clz.INSTANCE,
            I32Ctz.INSTANCE,
            I32Popcnt.INSTANCE,
            I32Add.INSTANCE,
            I32Sub.INSTANCE,
            I32Mul.INSTANCE,
            I32DivS.INSTANCE,
            I32DivU.INSTANCE,
            I32RemS.INSTANCE,
            I32RemU.INSTANCE,
            I32And.INSTANCE,
            I32Or.INSTANCE,
            I32Xor.INSTANCE,
            I32Shl.INSTANCE,
            I32ShrS.INSTANCE,
            I32ShrU.INSTANCE,
            I32Rotl.INSTANCE,
            I32Rotr.INSTANCE,

            I64Clz.INSTANCE,
            I64Ctz.INSTANCE,
            I64Popcnt.INSTANCE,
            I64Add.INSTANCE,
            I64Sub.INSTANCE,
            I64Mul.INSTANCE,
            I64DivS.INSTANCE,
            I64DivU.INSTANCE,
            I64RemS.INSTANCE,
            I64RemU.INSTANCE,
            I64And.INSTANCE,
            I64Or.INSTANCE,
            I64Xor.INSTANCE,
            I64Shl.INSTANCE,
            I64ShrS.INSTANCE,
            I64ShrU.INSTANCE,
            I64Rotl.INSTANCE,
            I64Rotr.INSTANCE,

            F32Abs.INSTANCE,
            F32Neg.INSTANCE,
            F32Ceil.INSTANCE,
            F32Floor.INSTANCE,
            F32Trunc.INSTANCE,
            F32Nearest.INSTANCE,
            F32Sqrt.INSTANCE,
            F32Add.INSTANCE,
            F32Sub.INSTANCE,
            F32Mul.INSTANCE,
            F32Div.INSTANCE,
            F32Min.INSTANCE,
            F32Max.INSTANCE,
            F32Copysign.INSTANCE,

            F64Abs.INSTANCE,
            F64Neg.INSTANCE,
            F64Ceil.INSTANCE,
            F64Floor.INSTANCE,
            F64Trunc.INSTANCE,
            F64Nearest.INSTANCE,
            F64Sqrt.INSTANCE,
            F64Add.INSTANCE,
            F64Sub.INSTANCE,
            F64Mul.INSTANCE,
            F64Div.INSTANCE,
            F64Min.INSTANCE,
            F64Max.INSTANCE,
            F64Copysign.INSTANCE,

            I32WrapI64.INSTANCE,
            I32TruncF32S.INSTANCE,
            I32TruncF32U.INSTANCE,
            I32TruncF64S.INSTANCE,
            I32TruncF64U.INSTANCE,
            I64ExtendI32S.INSTANCE,
            I64ExtendI32U.INSTANCE,
            I64TruncF32S.INSTANCE,
            I64TruncF32U.INSTANCE,
            I64TruncF64S.INSTANCE,
            I64TruncF64U.INSTANCE,
            F32ConvertI32S.INSTANCE,
            F32ConvertI32U.INSTANCE,
            F32ConvertI64S.INSTANCE,
            F32ConvertI64U.INSTANCE,
            F32DemoteF64.INSTANCE,
            F64ConvertI32S.INSTANCE,
            F64ConvertI32U.INSTANCE,
            F64ConvertI64S.INSTANCE,
            F64ConvertI64U.INSTANCE,
            F64PromoteF32.INSTANCE,
            I32ReinterpretF32.INSTANCE,
            I64ReinterpretF64.INSTANCE,
            F32ReinterpretI32.INSTANCE,
            F64ReinterpretI64.INSTANCE,

            I32Extend8S.INSTANCE,
            I32Extend16S.INSTANCE,
            I64Extend8S.INSTANCE,
            I64Extend16S.INSTANCE,
            I64Extend32S.INSTANCE,

            I32TruncSatF32S.INSTANCE,
            I32TruncSatF32U.INSTANCE,
            I32TruncSatF64S.INSTANCE,
            I32TruncSatF64U.INSTANCE,
            I64TruncSatF32S.INSTANCE,
            I64TruncSatF32U.INSTANCE,
            I64TruncSatF64S.INSTANCE,
            I64TruncSatF64U.INSTANCE,

            // Memory instructions
            DataDrop.INSTANCE
    );
}
