package net.janrupf.thunderwasm.test.wast;

import net.janrupf.thunderwasm.ThunderWasmException;
import net.janrupf.thunderwasm.assembler.WasmAssembler;
import net.janrupf.thunderwasm.assembler.WasmAssemblerException;
import net.janrupf.thunderwasm.assembler.emitter.WasmGenerators;
import net.janrupf.thunderwasm.assembler.emitter.objasm.ObjectWebASMClassFileEmitterFactory;
import net.janrupf.thunderwasm.instructions.InstructionRegistry;
import net.janrupf.thunderwasm.instructions.InstructionSet;
import net.janrupf.thunderwasm.module.WasmLoader;
import net.janrupf.thunderwasm.module.WasmModule;
import net.janrupf.thunderwasm.runtime.WasmModuleExports;
import net.janrupf.thunderwasm.runtime.linker.RuntimeLinker;
import net.janrupf.thunderwasm.runtime.linker.function.LinkedFunction;
import net.janrupf.thunderwasm.runtime.linker.global.*;
import net.janrupf.thunderwasm.test.TestClassLoader;
import net.janrupf.thunderwasm.test.wast.action.GetAction;
import net.janrupf.thunderwasm.test.wast.action.InvokeAction;
import net.janrupf.thunderwasm.test.wast.action.WastAction;
import net.janrupf.thunderwasm.test.wast.command.*;
import net.janrupf.thunderwasm.test.wast.linker.SpectestEnvironment;
import net.janrupf.thunderwasm.test.wast.linker.WastEnvironmentLinker;
import net.janrupf.thunderwasm.test.wast.value.WastValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Core test execution engine for WASM test suite.
 * Manages module loading, compilation, instantiation, and execution of WAST commands.
 */
public class WasmTestExecutor {
    private static final InstructionRegistry BASE_INSTRUCTION_REGISTRY = InstructionRegistry.builder()
            .with(InstructionSet.BASE)
            .build();

    private final Map<String, Object> namedModules;
    private final WastTestCollection collection;
    private final TestClassLoader testClassLoader;
    private final WastValueConverter valueConverter;
    private final SpectestEnvironment environment;
    private int assemblerCounter;

    private Object currentModule = null;
    private boolean isBroken;

    public WasmTestExecutor(WastTestCollection collection) {
        this.namedModules = new HashMap<>();
        this.collection = collection;
        this.isBroken = false;
        this.testClassLoader = new TestClassLoader();
        this.valueConverter = new WastValueConverter();
        this.environment = new SpectestEnvironment();
        this.assemblerCounter = 0;
    }

    /**
     * Determines whether the execution broke already.
     * <p>
     * WAST tests depend on each other - if one breaks, the remaining tests are
     * also broken.
     *
     * @return whether execution was broken already
     */
    public boolean isBroken() {
        return isBroken;
    }

    /**
     * Make the execution as broken.
     */
    public void markBroken() {
        this.isBroken = true;
    }

    /**
     * Retrieve the collection.
     *
     * @return the test collection
     */
    public WastTestCollection getCollection() {
        return collection;
    }

    /**
     * Executes a single WAST command and returns the result.
     *
     * @param command the command to execute
     * @throws Exception if the command execution fails
     */
    public void executeCommand(WastCommand command) throws Throwable {
        if (command instanceof ModuleCommand) {
            executeModuleCommand((ModuleCommand) command);
        } else if (command instanceof AssertReturnCommand) {
            executeAssertReturnCommand((AssertReturnCommand) command);
        } else if (command instanceof AssertTrapCommand) {
            executeAssertTrapCommand((AssertTrapCommand) command);
        } else if (command instanceof AssertInvalidCommand) {
            executeAssertInvalidCommand((AssertInvalidCommand) command);
        } else if (command instanceof AssertMalformedCommand) {
            executeAssertMalformedCommand((AssertMalformedCommand) command);
        } else if (command instanceof AssertUninstantiableCommand) {
            executeAssertUninstantiableCommand((AssertUninstantiableCommand) command);
        } else if (command instanceof AssertUnlinkableCommand) {
            executeAssertUnlinkableCommand((AssertUnlinkableCommand) command);
        } else if(command instanceof AssertExhaustionCommand) {
            executeAssertExhaustionCommand((AssertExhaustionCommand) command);
        } else if (command instanceof RegisterCommand) {
            executeRegisterCommand((RegisterCommand) command);
        } else if (command instanceof ActionCommand) {
            executeActionCommand((ActionCommand) command);
        } else {
            throw new UnsupportedOperationException("Unsupported command type: " + command.getClass().getSimpleName());
        }
    }

    /**
     * Loads and instantiates a WASM module.
     */
    private void executeModuleCommand(ModuleCommand command) throws Exception {
        WasmAssembler assembler = loadAndMakeAssembler(command.getFilename());

        byte[] clazz = assembler.assembleToModule();
        Class<?> moduleClazz = testClassLoader.loadFromBytes(null, clazz);

        Constructor<?> constructor = moduleClazz.getConstructor(RuntimeLinker.class);
        this.currentModule = constructor.newInstance(new WastEnvironmentLinker(environment, this.namedModules));
        
        if (command.isNamed()) {
            namedModules.put(command.getName(), this.currentModule);
        }
    }
    
    /**
     * Executes an action and validates the return values.
     */
    private void executeAssertReturnCommand(AssertReturnCommand command) throws Throwable {
        Object result = executeAction(command.getAction());
        checkExpectedReturn(currentModule, result, command.getExpected());
    }
    
    /**
     * Executes an action and validates that it traps with the expected message.
     */
    private void executeAssertTrapCommand(AssertTrapCommand command) throws Exception {
        Throwable thrownException = null;
        try {
            executeAction(command.getAction());
        } catch (Throwable e) {
            thrownException = e;
        }

        // NOTE: We can't check the expected values of the trap command as the operand
        // stack is not externally observable after transpilation to JVM bytecode
        Assertions.assertNotNull(thrownException, "expected to trap (" + command.getText() + ")");
        WastExceptionMatcher.checkTrap(thrownException, command.getText());
    }
    
    /**
     * Validates that a module fails to load as invalid.
     */
    private void executeAssertInvalidCommand(AssertInvalidCommand command) throws Exception {
        WasmAssembler assembler = loadAndMakeAssembler(command.getFilename());

        try {
            assembler.assembleToModule();
            Assertions.fail("expected assembleToModule to throw (" + command.getText() + ")");
        } catch (WasmAssemblerException ignored) {}
    }
    
    /**
     * Validates that a module fails to load as malformed.
     */
    private void executeAssertMalformedCommand(AssertMalformedCommand command) throws Exception {
        if (command.getModuleType().equals("text")) {
            Assumptions.abort("Text module parsing is not supported by Thunder WASM");
        }

        if (!command.getModuleType().equals("binary")) {
            throw new IllegalStateException(
                    "Don't know how to handle assert_malformed with module type " + command.getModuleType());
        }


        try {
            loadModuleFromResource(command.getFilename());
            Assertions.fail("Expected WasmLoader.load() to throw");
        } catch (WasmAssemblerException e) {}
    }
    
    /**
     * Validates that a module fails to instantiate.
     */
    private void executeAssertUninstantiableCommand(AssertUninstantiableCommand command) throws Throwable {
        WasmAssembler assembler = loadAndMakeAssembler(command.getFilename());

        byte[] clazz = assembler.assembleToModule();
        Class<?> moduleClazz = testClassLoader.loadFromBytes(null, clazz);

        Constructor<?> constructor = moduleClazz.getConstructor(RuntimeLinker.class);
        try {
            constructor.newInstance(new WastEnvironmentLinker(environment, this.namedModules));
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof WastEnvironmentLinker.LinkageFailedException) {
                throw e.getTargetException();
            }

            WastExceptionMatcher.checkTrap(e.getTargetException(), command.getText());
        }
    }
    
    /**
     * Validates that a module fails to link.
     */
    private void executeAssertUnlinkableCommand(AssertUnlinkableCommand command) throws Throwable {
        WasmAssembler assembler = loadAndMakeAssembler(command.getFilename());

        byte[] clazz = assembler.assembleToModule();
        Class<?> moduleClazz = testClassLoader.loadFromBytes(null, clazz);

        Constructor<?> constructor = moduleClazz.getConstructor(RuntimeLinker.class);
        try {
            constructor.newInstance(new WastEnvironmentLinker(environment, this.namedModules));
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (!(targetException instanceof WastEnvironmentLinker.LinkageFailedException)) {
                throw targetException;
            }

            Assertions.assertEquals(command.getText(), targetException.getMessage());
        }
    }


    private void executeAssertExhaustionCommand(AssertExhaustionCommand command) throws Throwable {
        try {
            executeAction(command.getAction());
        } catch (Throwable t) {
            WastExceptionMatcher.checkTrap(t, command.getText());
        }
    }


    /**
     * Registers a module's exports for cross-module calls.
     */
    private void executeRegisterCommand(RegisterCommand command) throws Throwable {
        // Get the current module to register
        Object moduleToRegister = currentModule;
        if (moduleToRegister == null) {
            throw new IllegalStateException("No current module to register");
        }
        
        // Register the module with the given name
        namedModules.put(command.getAs(), moduleToRegister);
    }
    
    /**
     * Executes a standalone action command.
     */
    private void executeActionCommand(ActionCommand command) throws Throwable {
        Object module;
        if (command.getAction().getModule() == null) {
            module = this.currentModule;
        } else {
            module = this.namedModules.get(command.getAction().getModule());
        }

        Object value = executeAction(command.getAction());
        checkExpectedReturn(module, value, command.getExpected());
    }

    /**
     * Executes a WAST action (function call, global get, etc.).
     */
    private Object executeAction(WastAction action) throws Throwable {
        Object module;
        if (action.getModule() == null) {
            module = this.currentModule;
        } else {
            module = this.namedModules.get(action.getModule());
        }

        if (module == null) {
            throw new IllegalStateException("No module to execute the action on");
        }

        if (action instanceof InvokeAction) {
            return executeInvokeAction(module, (InvokeAction) action);
        } else if (action instanceof GetAction) {
            return executeGetAction(module, (GetAction) action);
        } else {
            throw new UnsupportedOperationException("Unsupported action type: " + action.getClass().getSimpleName());
        }
    }
    
    /**
     * Executes a function call action.
     */
    private Object executeInvokeAction(Object targetModule, InvokeAction action) throws Throwable {
        LinkedFunction function = getExport(LinkedFunction.class, targetModule, action.getField());
        List<Object> javaArguments = this.valueConverter.convertToJavaValues(targetModule, action.getArgs());
        
        // Invoke the function
        return function.asMethodHandle().invokeWithArguments(javaArguments.toArray());
    }

    private Object executeGetAction(Object targetModule, GetAction action) {
        LinkedGlobalBase global = getExport(LinkedGlobalBase.class, targetModule, action.getField());

        if (global instanceof LinkedReadOnlyIntGlobal) {
            return ((LinkedReadOnlyIntGlobal) global).get();
        } else if (global instanceof LinkedReadOnlyLongGlobal) {
            return ((LinkedReadOnlyLongGlobal) global).get();
        } else if (global instanceof LinkedReadOnlyFloatGlobal) {
            return ((LinkedReadOnlyFloatGlobal) global).get();
        } else if (global instanceof LinkedReadOnlyDoubleGlobal) {
            return ((LinkedReadOnlyDoubleGlobal) global).get();
        } else if (global instanceof LinkedReadOnlyObjectGlobal) {
            return ((LinkedReadOnlyObjectGlobal<?>) global).get();
        } else {
            throw new IllegalStateException("Unsupported global type: " + global.getClass().getSimpleName());
        }
    }

    private <T> T getExport(Class<T> clazz, Object module, String name) {
        Map<String, Object> exports = ((WasmModuleExports) module).getExports();

        if (!exports.containsKey(name)) {
            throw new IllegalStateException("No export named " + name);
        }

        try {
            return clazz.cast(exports.get(name));
        } catch (ClassCastException e) {
            throw new IllegalStateException("Export " + name + " is not a " + clazz.getSimpleName(), e);
        }
    }

    private void checkExpectedReturn(Object module, Object returnValue, List<WastValue> expectedValues) {
        List<Object> expectedJavaValues = this.valueConverter.convertToJavaValues(module, expectedValues);

        List<Object> returnedValues;
        if (returnValue == null) {
            // A null return is equivalent to not returning any value - "null" references
            // still return a function or extern reference wrapper object
            returnedValues = Collections.emptyList();
        } else {
            returnedValues = Collections.singletonList(returnValue);
        }

        Assertions.assertArrayEquals(expectedJavaValues.toArray(), returnedValues.toArray());
    }

    private WasmAssembler loadAndMakeAssembler(String fileName) throws ThunderWasmException, IOException {
        return makeAssembler(loadModuleFromResource(fileName));
    }

    private WasmModule loadModuleFromResource(String fileName) throws ThunderWasmException, IOException {
        try(InputStream stream = this.collection.readResource(fileName)) {
            WasmLoader loader = new WasmLoader(stream, BASE_INSTRUCTION_REGISTRY);

            return loader.load();
        }
    }

    private WasmAssembler makeAssembler(WasmModule module) {
        return new WasmAssembler(
                module,
                new ObjectWebASMClassFileEmitterFactory(),
                "net.janrupf.thunderwasm.test.wast.module",
                "WastCollectionModule$$" + collection.getName().replaceAll("(\\.;/_)", "$") + "$" + (assemblerCounter++),
                new WasmGenerators()
        );
    }
}