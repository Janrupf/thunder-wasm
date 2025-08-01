package net.janrupf.thunderwasm.test.util;

import net.janrupf.thunderwasm.ThunderWasmException;
import net.janrupf.thunderwasm.assembler.WasmAssembler;
import net.janrupf.thunderwasm.assembler.WasmAssemblerConfiguration;
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
import net.janrupf.thunderwasm.test.TestClassLoader;
import net.janrupf.thunderwasm.types.FunctionType;
import net.janrupf.thunderwasm.types.ValueType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.ThrowingConsumer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    public static WasmModule loadFromFile(String path) throws IOException, InvalidModuleException {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(Paths.get(path)))) {
            WasmLoader loader = new WasmLoader(in, BASE_INSTRUCTION_REGISTRY);
            return loader.load();
        }
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

    public static WasmAssembler makeAssembler(WasmModule module) throws WasmAssemblerException {
        return makeAssembler(module, WasmAssemblerConfiguration.DEFAULT);
    }

    public static WasmAssembler makeAssembler(
            WasmModule module,
            WasmAssemblerConfiguration configuration
    ) throws WasmAssemblerException {
        int counter = ASSEMBLER_COUNTER.getAndIncrement();

        return new WasmAssembler(
                module,
                new ObjectWebASMClassFileEmitterFactory(),
                "net.janrupf.thunderwasm.generated",
                "TestModule" + counter,
                new WasmGenerators(),
                configuration
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
        long startTime = System.currentTimeMillis();
        byte[] result = assembler.assembleToModule();
        long endTime = System.currentTimeMillis();

        long duration = endTime - startTime;
        System.out.println("Assembling took " + duration + "ms");

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

        Class<?>[] realArgumentTypes = new Class<?>[argumentTypes.length + 1];
        System.arraycopy(argumentTypes, 0, realArgumentTypes, 0, argumentTypes.length);
        realArgumentTypes[argumentTypes.length] = moduleClass;

        Method method;
        try {
            method = moduleClass.getMethod("$code_" + index, realArgumentTypes);
        } catch (NoSuchMethodException e) {
            throw new ThunderWasmException("Failed to look up method", e);
        }

        try {
            method.setAccessible(true);
        } catch (Exception e) {
            throw new ThunderWasmException("Failed to make method accessible", e);
        }

        Object[] realArguments = new Object[arguments.length + 1];
        System.arraycopy(arguments, 0, realArguments, 0, arguments.length);
        realArguments[arguments.length] = moduleInstance;

        try {
            return method.invoke(null, realArguments);
        } catch (Exception e) {
            throw new ThunderWasmException("Failed to invoke method", e);
        }
    }
}
