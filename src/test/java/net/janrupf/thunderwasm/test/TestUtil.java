package net.janrupf.thunderwasm.test;

import net.janrupf.thunderwasm.ThunderWasmException;
import net.janrupf.thunderwasm.assembler.WasmAssembler;
import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.WasmGenerators;
import net.janrupf.thunderwasm.assembler.emitter.objasm.ObjectWebASMClassFileEmitterFactory;
import net.janrupf.thunderwasm.instructions.InstructionInstance;
import net.janrupf.thunderwasm.instructions.InstructionRegistry;
import net.janrupf.thunderwasm.instructions.InstructionSet;
import net.janrupf.thunderwasm.instructions.WasmInstruction;
import net.janrupf.thunderwasm.module.InvalidModuleException;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.module.WasmModule;
import net.janrupf.thunderwasm.module.encoding.LargeArray;
import net.janrupf.thunderwasm.module.section.WasmSection;
import net.janrupf.thunderwasm.runtime.linker.RuntimeLinker;
import net.janrupf.thunderwasm.types.FunctionType;
import net.janrupf.thunderwasm.types.ValueType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.ThrowingConsumer;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

public class TestUtil {
    private static final InstructionRegistry BASE_INSTRUCTION_REGISTRY = InstructionRegistry.builder()
            .with(InstructionSet.BASE)
            .build();

    private static final TestClassLoader TEST_CLASS_LOADER = new TestClassLoader();

    public static InputStream wasmResource(String name) {
        InputStream stream = TestUtil.class.getClassLoader().getResourceAsStream(name);
        if (stream == null) {
            throw new RuntimeException("Resource not found: " + name);
        }

        return stream;
    }

    public static WasmLoader loaderFor(String name) {
        return new WasmLoader(wasmResource(name), BASE_INSTRUCTION_REGISTRY);
    }

    public static WasmModule load(String name) throws IOException, InvalidModuleException {
        WasmLoader loader = TestUtil.loaderFor(name);
        return loader.load();
    }

    @SuppressWarnings("unchecked")
    public static <T extends WasmSection> T getSection(WasmModule module, byte id) {
        return module.getSections().stream()
                .filter(section -> section.getId() == id)
                .map(section -> (T) section)
                .findFirst()
                .orElse(null);
    }

    public static void checkFunctionType(FunctionType fnType, ValueType[] inputs, ValueType[] outputs) {
        LargeArray<ValueType> fnInputs = fnType.getInputs();
        LargeArray<ValueType> fnOutputs = fnType.getOutputs();

        Assertions.assertArrayEquals(inputs, fnInputs.asFlatArray());
        Assertions.assertArrayEquals(outputs, fnOutputs.asFlatArray());
    }

    @SuppressWarnings("unchecked")
    public static <T extends WasmInstruction.Data> void checkInstruction(
            InstructionInstance actual,
            WasmInstruction<T> expected,
            ThrowingConsumer<T> dataChecker
    ) {
        Assertions.assertEquals(expected, actual.getInstruction());
        try {
            dataChecker.accept((T) actual.getData());
        } catch (Throwable e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    private static final AtomicInteger ASSEMBLER_COUNTER = new AtomicInteger(0);

    public static WasmAssembler makeAssembler(WasmModule module) {
        int counter = ASSEMBLER_COUNTER.getAndIncrement();

        return new WasmAssembler(
                module,
                new ObjectWebASMClassFileEmitterFactory(),
                "net.janrupf.thunderwasm.generated",
                "TestModule" + counter,
                new WasmGenerators()
        );
    }

    public static Object instantiateModule(
            WasmModule module,
            RuntimeLinker linker
    ) throws WasmAssemblerException {
        WasmAssembler assembler = TestUtil.makeAssembler(module);
        return instantiateModule(assembler, linker);
    }

    public static Object instantiateModule(
            WasmAssembler assembler,
            RuntimeLinker linker
    ) throws WasmAssemblerException {
        byte[] result = assembler.assembleToModule();
        return instantiateModule(assembler, result, linker);
    }

    public static Object instantiateModule(
            WasmAssembler assembler,
            byte[] classData,
            RuntimeLinker linker
    ) throws WasmAssemblerException {
        Class<?> loaded;
        try {
            loaded = TEST_CLASS_LOADER.loadFromBytes(null, classData);
        } catch (Exception e) {
            throw new WasmAssemblerException("Failed to load generated class", e);
        }

        Object instance;
        try {
            instance = loaded.getConstructor(RuntimeLinker.class).newInstance(linker);
        } catch (Exception e) {
            throw new WasmAssemblerException("Failed to instantiate generated class", e);
        }

        return instance;
    }

    public static Object callCodeMethod(
            Object moduleInstance,
            long index,
            Class<?>[] argumentTypes,
            Object[] arguments
    ) throws ThunderWasmException  {
        Class<?> moduleClass = moduleInstance.getClass();

        Method method;
        try {
            method = moduleClass.getMethod("$code_" + index, argumentTypes);
        } catch (NoSuchMethodException e) {
            throw new ThunderWasmException("Failed to look up method", e);
        }

        try {
            method.setAccessible(true);
        } catch (Exception e) {
            throw new ThunderWasmException("Failed to make method accessible", e);
        }

        try {
            return method.invoke(moduleInstance, arguments);
        } catch (Exception e) {
            throw new ThunderWasmException("Failed to invoke method", e);
        }
    }
}
