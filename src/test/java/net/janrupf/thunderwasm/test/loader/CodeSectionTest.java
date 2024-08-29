package net.janrupf.thunderwasm.test.loader;

import net.janrupf.thunderwasm.instructions.Expr;
import net.janrupf.thunderwasm.instructions.Function;
import net.janrupf.thunderwasm.instructions.InstructionInstance;
import net.janrupf.thunderwasm.instructions.Local;
import net.janrupf.thunderwasm.instructions.control.Block;
import net.janrupf.thunderwasm.instructions.control.Call;
import net.janrupf.thunderwasm.instructions.control.Nop;
import net.janrupf.thunderwasm.instructions.numeric.I32Const;
import net.janrupf.thunderwasm.module.WasmModule;
import net.janrupf.thunderwasm.module.encoding.LargeArrayIndex;
import net.janrupf.thunderwasm.module.section.CodeSection;
import net.janrupf.thunderwasm.test.TestUtil;
import net.janrupf.thunderwasm.types.BlockType;
import net.janrupf.thunderwasm.types.NumberType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class CodeSectionTest {
    @Test
    public void testCodeSection() throws Exception {
        WasmModule module = TestUtil.load("code-section.wasm");

        CodeSection codeSection = TestUtil.getSection(module, (byte) 10);

        Function firstFunction = codeSection.getFunctions().get(LargeArrayIndex.ZERO);
        Local[] locals = firstFunction.getLocals().asFlatArray();

        Assertions.assertEquals(2, locals.length);
        Assertions.assertEquals(2, locals[0].getCount());
        Assertions.assertEquals(NumberType.I32, locals[0].getType());
        Assertions.assertEquals(1, locals[1].getCount());
        Assertions.assertEquals(NumberType.F64, locals[1].getType());

        Expr functionExpr = firstFunction.getExpr();
        List<InstructionInstance> topLevelInstructions = functionExpr.getInstructions();
        Assertions.assertEquals(1, topLevelInstructions.size());

        TestUtil.checkInstruction(
                topLevelInstructions.get(0),
                Block.INSTANCE,
                (data) -> {
                    Assertions.assertInstanceOf(BlockType.Value.class, data.getType());

                    BlockType.Value blockType = (BlockType.Value) data.getType();
                    Assertions.assertEquals(NumberType.I32, blockType.getValueType());

                    Expr primaryExpression = data.getPrimaryExpression();
                    List<InstructionInstance> blockInstructions = primaryExpression.getInstructions();

                    Assertions.assertEquals(3, blockInstructions.size());
                    TestUtil.checkInstruction(
                            blockInstructions.get(0),
                            Nop.INSTANCE,
                            (blockData) -> {
                            }
                    );
                    TestUtil.checkInstruction(
                            blockInstructions.get(1),
                            I32Const.INSTANCE,
                            (blockData) -> Assertions.assertEquals(42, blockData.getValue())
                    );
                    TestUtil.checkInstruction(
                            blockInstructions.get(2),
                            Call.INSTANCE,
                            (blockData) -> {
                                Assertions.assertEquals(0, blockData.getFunctionIndex());
                            }
                    );
                }
        );
    }
}
